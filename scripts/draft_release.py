#!/usr/bin/env python3
"""Verify the four native packages and upload them to a draft GitHub Release."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def gh(*arguments):
    return subprocess.run(["gh", *map(str, arguments)], cwd=ROOT, check=True)


def main():
    version = os.environ.get("SCOREMANAGER_VERSION", "4.6.0").removeprefix("v")
    if not re.fullmatch(r"[1-9][0-9]*\.[0-9]+\.[0-9]+", version):
        raise ValueError("Invalid release version")
    tag = "v" + version
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    dist = ROOT / "dist"
    assets, checksums = [], []
    for target, extension in (("windows-x64", ".zip"), ("linux-x64", ".tar.gz"),
                              ("macos-arm64", ".zip"), ("macos-x64", ".zip")):
        name = "ScoreManager-" + version + "-" + target
        archive = dist / (name + extension)
        digest = hashlib.sha256()
        with archive.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        checksum_file = dist / (archive.name + ".sha256")
        expected = checksum_file.read_text(encoding="ascii").strip()
        actual = digest.hexdigest() + "  " + archive.name
        if expected != actual:
            raise ValueError("Archive checksum mismatch: " + archive.name)
        report = dist / (name + "-BUILD-INFO.json")
        info = json.loads(report.read_text(encoding="utf-8"))
        if info["source"]["commit"] != commit or info["source"]["working_tree_modified"]:
            raise ValueError("Package is not from the expected clean source commit: " + archive.name)
        if not info["swing_startup_smoke_passed"] or not info["native_launcher_startup_smoke_passed"]:
            raise ValueError("Package has not passed startup tests: " + archive.name)
        assets += [archive, checksum_file, report]
        checksums.append(actual)
    checksum_list = dist / "SHA256SUMS.txt"
    checksum_list.write_text("\n".join(checksums) + "\n", encoding="ascii")
    assets.append(checksum_list)
    notes = dist / "release-notes.md"
    notes.write_text(
        "ScoreManager " + version + "\n\n"
        "中文：Windows x64、Linux x64、macOS 苹果芯片与 Intel 原生运行包，均内置 Java 21。"
        "解压整个包后运行；安装和使用说明、50 人示例 CSV 均在包内。首次使用请自行创建管理员账号。\n\n"
        "English: Native packages for Windows x64, Linux x64, macOS Apple Silicon and Intel, all with Java 21 included. "
        "Extract the complete archive to run. Bilingual installation/user guides and a 50-student sample CSV are included. "
        "Create an administrator account at first launch.\n\n"
        "Each package passed compilation, regression tests on its bundled runtime, Swing window startup, and native-launcher/database startup checks on GitHub runners. "
        "This does not replace full manual testing on every supported OS version. Packages are not publisher-signed or Apple-notarized.\n\n"
        "This release remains a draft until reviewed. Verify downloads with SHA256SUMS.txt.\n",
        encoding="utf-8")
    existing = subprocess.run(["gh", "release", "view", tag, "--json", "isDraft,targetCommitish"],
                              cwd=ROOT, text=True, capture_output=True)
    if existing.returncode == 0:
        state = json.loads(existing.stdout)
        if not state["isDraft"]:
            raise RuntimeError("Refusing to replace files in an already published release")
        if state["targetCommitish"] not in (commit, os.environ.get("GITHUB_REF_NAME")):
            raise RuntimeError("Existing draft targets a different commit; inspect it before replacing assets")
        gh("release", "edit", tag, "--draft", "--notes-file", notes)
        gh("release", "upload", tag, *assets, "--clobber")
    else:
        gh("release", "create", tag, *assets, "--target", commit, "--draft", "--title", "ScoreManager " + version,
           "--notes-file", notes)


if __name__ == "__main__":
    main()
