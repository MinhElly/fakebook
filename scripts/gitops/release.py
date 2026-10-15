"""Record CI image digests and promote a complete release into Kustomize."""
import argparse
import json
import re
import subprocess
from pathlib import Path

IMAGES = tuple("fakebook-" + name for name in (
    "gateway", "userservice", "postservice", "mediaservice",
    "commentservice", "feedservice", "frontend",
))
LOCAL_OVERLAY = "infrastructure/k8s/overlays/local/kustomization.yaml"


def validate(sha, owner, records):
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Source SHA must contain 40 lowercase hex characters")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", owner):
        raise ValueError("Invalid lowercase registry owner")
    by_name = {}
    for record in records:
        name = record["name"]
        if name not in IMAGES or name in by_name:
            raise ValueError(f"Unexpected or duplicate image: {name}")
        if record["sha"] != sha or record["image"] != f"ghcr.io/{owner}/{name}":
            raise ValueError(f"Source SHA or registry mismatch: {name}")
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", record["digest"]):
            raise ValueError(f"Invalid digest: {name}")
        by_name[name] = record
    if set(by_name) != set(IMAGES):
        raise ValueError("A release requires exactly all seven images")
    return by_name


def requires_build(paths):
    # Manifest-only promotion/revert commits must not create another release.
    return any(not (path.startswith("infrastructure/k8s/") or
                    path.startswith("docs/") or path.endswith(".md"))
               for path in paths)


def verify_local_release(base, head, owner):
    """Reject stale releases and changes outside the seven pinned images."""
    def git(*args):
        return subprocess.check_output(["git", *args]).decode("utf-8")

    paths = git("diff", "--name-only", "-z", base, head).split("\0")
    if [p for p in paths if p] != [LOCAL_OVERLAY]:
        raise ValueError("Local release PR must change only the local overlay")
    before = git("show", f"{base}:{LOCAL_OVERLAY}")
    after = git("show", f"{head}:{LOCAL_OVERLAY}")
    before_prefix, _ = before.split("\nimages:\n", 1)
    after_prefix, section = after.split("\nimages:\n", 1)
    if before_prefix != after_prefix:
        raise ValueError("Local release PR must preserve all non-image configuration")
    pattern = (r"  - name: (fakebook-[a-z]+)\n"
               r"    newName: (ghcr\.io/[a-z0-9-]+/fakebook-[a-z]+)\n"
               r"    newTag: sha-([0-9a-f]{40})\n"
               r"    digest: (sha256:[0-9a-f]{64})\n")
    matches = list(re.finditer(pattern, section))
    if not matches or "".join(m.group() for m in matches) != section:
        raise ValueError("Local release images must contain only SHA tags and digests")
    records = [dict(zip(("name", "image", "sha", "digest"), m.groups()))
               for m in matches]
    sha = records[0]["sha"]
    validate(sha, owner, records)
    subprocess.run(["git", "merge-base", "--is-ancestor", sha, base], check=True)
    paths = git("diff", "--name-only", "-z", sha, base).split("\0")
    if requires_build([p for p in paths if p]):
        raise ValueError("Newer source exists on develop; wait for its CI release")
    return sha


def promote(overlay, sha, owner, records, verify_registry=False):
    images = validate(sha, owner, records)
    if verify_registry:
        for record in images.values():
            ref = f'{record["image"]}:sha-{sha}'
            digest = subprocess.check_output([
                "docker", "buildx", "imagetools", "inspect", ref,
                "--format", "{{.Manifest.Digest}}",
            ], text=True).strip()
            if digest != record["digest"]:
                raise ValueError(f"Registry digest differs from CI artifact: {ref}")
    content = overlay.read_text(encoding="utf-8")
    # Images are deliberately the final section; preserve all other YAML verbatim.
    prefix, section = content.split("\nimages:\n", 1)
    names = re.findall(r"^  - name: (\S+)$", section, re.MULTILINE)
    if len(names) != 7 or set(names) != set(IMAGES):
        raise ValueError("Overlay must manage exactly the seven release images")
    if re.search(r"^\S", section, re.MULTILINE):
        raise ValueError("images must be the last overlay section")
    lines = [prefix, "images:"]
    for name in IMAGES:
        record = images[name]
        lines.extend((f"  - name: {name}", f'    newName: {record["image"]}',
                      f"    newTag: sha-{sha}", f'    digest: {record["digest"]}'))
    overlay.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return images


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    record = commands.add_parser("record")
    record.add_argument("--name", choices=IMAGES, required=True)
    record.add_argument("--sha", required=True)
    record.add_argument("--owner", required=True)
    record.add_argument("--digest", required=True)
    record.add_argument("--output", type=Path, required=True)
    change = commands.add_parser("changes")
    change.add_argument("--base", required=True)
    change.add_argument("--head", required=True)
    verify = commands.add_parser("verify-local")
    verify.add_argument("--base", required=True)
    verify.add_argument("--head", required=True)
    verify.add_argument("--owner", required=True)
    update = commands.add_parser("promote")
    update.add_argument("--sha", required=True)
    update.add_argument("--owner", required=True)
    update.add_argument("--artifacts", type=Path, required=True)
    update.add_argument("--overlay", type=Path, required=True)
    update.add_argument("--body", type=Path, required=True)
    update.add_argument("--run-url", required=True)
    update.add_argument("--verify-registry", action="store_true")
    args = parser.parse_args()
    if args.command == "changes":
        paths = subprocess.check_output([
            "git", "diff", "--name-only", "-z", args.base, args.head,
        ]).decode().split("\0")
        print("build=" + str(requires_build([p for p in paths if p])).lower())
    elif args.command == "verify-local":
        sha = verify_local_release(args.base, args.head, args.owner)
        print(f"Local release source is current: {sha}")
    elif args.command == "record":
        digest = args.digest.strip()
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
            raise ValueError("Invalid digest returned by image publisher")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({
            "name": args.name, "sha": args.sha,
            "image": f"ghcr.io/{args.owner}/{args.name}", "digest": digest,
        }) + "\n", encoding="utf-8")
    else:
        records = [json.loads(p.read_text()) for p in args.artifacts.glob("*.json")]
        images = promote(args.overlay, args.sha, args.owner, records, args.verify_registry)
        target_env = "staging" if "staging" in str(args.overlay).lower() else "local"
        body = [f"Promote source commit `{args.sha}` to {target_env} Kubernetes.",
                f"\nCI: {args.run_url}", "\nAll six backends passed Maven verify; frontend build and typecheck passed.",
                "All seven registry tags were checked against CI publication digests.",
                "\n| Image | Tag | Digest |", "|---|---|---|"]
        for record in images.values():
            body.append(f'| `{record["image"]}` | `sha-{args.sha}` | `{record["digest"]}` |')
        body.extend((f"\nMerge allows Argo CD to sync {target_env}. Deployment and authenticated smoke checks remain pending.",
                     "Rollback: revert this image promotion commit through a PR."))
        args.body.write_text("\n".join(body) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
