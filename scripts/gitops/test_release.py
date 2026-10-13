import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from release import IMAGES, promote, requires_build, validate

SHA = "a" * 40
DIGEST = "sha256:" + "b" * 64


def records():
    return [dict(name=name, sha=SHA, image=f"ghcr.io/minhelly/{name}", digest=DIGEST)
            for name in IMAGES]


class ReleaseTests(unittest.TestCase):
    def test_promotion_and_rollback_commits_do_not_build(self):
        self.assertFalse(requires_build(["infrastructure/k8s/overlays/local/kustomization.yaml"]))
        self.assertFalse(requires_build(["docs/06-deployment/gitops-release.md"]))
        self.assertFalse(requires_build([]))
        self.assertTrue(requires_build(["frontend/src/App.tsx"]))
        self.assertTrue(requires_build([".github/workflows/ci.yml"]))
        self.assertTrue(requires_build(["scripts/gitops/release.py"]))
        self.assertTrue(requires_build(["gateway/pom.xml", "infrastructure/k8s/base/gateway.yaml"]))

    def test_incomplete_duplicate_or_wrong_release_is_rejected(self):
        invalid = [records()[:-1], records() + [records()[0]]]
        for field, value in (("sha", "c" * 40), ("digest", "latest"),
                             ("image", "ghcr.io/other/fakebook-gateway")):
            modified = records()
            modified[0][field] = value
            invalid.append(modified)
        for entries in invalid:
            with self.subTest(entries=entries), self.assertRaises(ValueError):
                validate(SHA, "minhelly", entries)
        with self.assertRaises(ValueError):
            validate("a" * 7, "minhelly", records())

    def test_promotion_preserves_resources_and_is_idempotent(self):
        with tempfile.TemporaryDirectory() as folder:
            overlay = Path(folder) / "kustomization.yaml"
            prefix = "apiVersion: kustomize.config.k8s.io/v1beta1\nresources:\n  - ../../base\n"
            overlay.write_text(prefix + "\nimages:\n" + "".join(
                f"  - name: {name}\n    newTag: local-v1\n" for name in IMAGES))
            promote(overlay, SHA, "minhelly", records())
            first = overlay.read_text()
            self.assertTrue(first.startswith(prefix))
            self.assertEqual(first.count(f"digest: {DIGEST}"), 7)
            self.assertEqual(first.count(f"newTag: sha-{SHA}"), 7)
            promote(overlay, SHA, "minhelly", records())
            self.assertEqual(first, overlay.read_text())

    def test_registry_mismatch_cannot_modify_overlay(self):
        with tempfile.TemporaryDirectory() as folder:
            overlay = Path(folder) / "kustomization.yaml"
            original = "unchanged"
            overlay.write_text(original)
            with patch("release.subprocess.check_output", return_value="sha256:" + "c" * 64):
                with self.assertRaisesRegex(ValueError, "differs from CI"):
                    promote(overlay, SHA, "minhelly", records(), verify_registry=True)
            self.assertEqual(overlay.read_text(), original)

    def test_registry_checks_all_seven_tags(self):
        with tempfile.TemporaryDirectory() as folder:
            overlay = Path(folder) / "kustomization.yaml"
            overlay.write_text("resources: []\n\nimages:\n" + "".join(
                f"  - name: {name}\n    newTag: old\n" for name in IMAGES))
            with patch("release.subprocess.check_output", return_value=DIGEST + "\n") as inspect:
                promote(overlay, SHA, "minhelly", records(), verify_registry=True)
            self.assertEqual(inspect.call_count, 7)
            for call in inspect.call_args_list:
                self.assertIn(f":sha-{SHA}", call.args[0][4])


if __name__ == "__main__":
    unittest.main()
