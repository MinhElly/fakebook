"""Exercise the release freshness gate against real Git histories."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from release import IMAGES, LOCAL_OVERLAY

SCRIPT = Path(__file__).with_name("release.py").resolve()


class LocalReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.git("init", "-q")
        self.overlay = self.repo / LOCAL_OVERLAY
        self.overlay.parent.mkdir(parents=True)
        self.prefix = "resources:\n  - ../../base\n\nimages:\n"
        self.overlay.write_text(self.images("a" * 40), encoding="utf-8")
        (self.repo / "app.txt").write_text("source A", encoding="utf-8")
        self.source = self.commit("source A")
        self.overlay.write_text(self.images(self.source), encoding="utf-8")

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.repo).decode().strip()

    def commit(self, message):
        self.git("add", ".")
        self.git("-c", "user.name=Release test", "-c", "user.email=test@example.invalid",
                 "-c", "commit.gpgsign=false", "commit", "-qm", message)
        return self.git("rev-parse", "HEAD")

    def images(self, sha):
        return self.prefix + "".join(
            f"  - name: {name}\n    newName: ghcr.io/minhelly/{name}\n"
            f"    newTag: sha-{sha}\n    digest: sha256:{'b' * 64}\n"
            for name in IMAGES)

    def verify(self, base=None):
        head = self.commit("release")
        return subprocess.run([sys.executable, str(SCRIPT), "verify-local",
                               "--base", base or self.source, "--head", head,
                               "--owner", "minhelly"], cwd=self.repo,
                              capture_output=True, text=True)

    def test_current_source_is_accepted(self):
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(self.source, result.stdout)

    def test_documentation_after_source_is_allowed(self):
        self.overlay.write_text(self.images("a" * 40), encoding="utf-8")
        (self.repo / "README.md").write_text("updated docs", encoding="utf-8")
        base = self.commit("documentation")
        self.overlay.write_text(self.images(self.source), encoding="utf-8")
        result = self.verify(base)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_newer_source_blocks_even_an_updated_release_branch(self):
        self.overlay.write_text(self.images("a" * 40), encoding="utf-8")
        (self.repo / "app.txt").write_text("source B", encoding="utf-8")
        base = self.commit("new source B")
        self.overlay.write_text(self.images(self.source), encoding="utf-8")
        result = self.verify(base)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Newer source exists", result.stderr)

    def test_extra_file_is_rejected(self):
        (self.repo / "unexpected.txt").write_text("not an image", encoding="utf-8")
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("only the local overlay", result.stderr)

    def test_non_image_configuration_is_rejected(self):
        self.overlay.write_text(self.images(self.source).replace("../../base", "../../other"),
                                encoding="utf-8")
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("non-image configuration", result.stderr)

    def test_mixed_source_and_wrong_registry_are_rejected(self):
        for label, content, error in (
            ("mixed", self.images(self.source).replace(self.source, "c" * 40, 1),
             "Source SHA or registry mismatch"),
            ("registry", self.images(self.source).replace("ghcr.io/minhelly/", "ghcr.io/other/", 1),
             "Source SHA or registry mismatch"),
            ("missing image", self.images(self.source).rsplit("  - name:", 1)[0],
             "exactly all seven images"),
            ("extra field", self.images(self.source) + "    extra: value\n",
             "only SHA tags and digests"),
        ):
            with self.subTest(label=label):
                self.overlay.write_text(content, encoding="utf-8")
                result = self.verify()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(error, result.stderr)


if __name__ == "__main__":
    unittest.main()
