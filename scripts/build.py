#!/usr/bin/env python3
"""Build a native, self-contained ScoreManager release using Python 3.9+ and JDK 21.

Run on the target OS/CPU; cross-compilation is deliberately not supported.
No network access, external Python packages, or user databases are needed.
"""
import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import sqlite3
import subprocess
import tarfile
import tempfile
import time
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = "java.desktop,java.sql,java.logging,java.naming,java.management,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.crypto.ec"
DEVELOPMENT_SOURCES = {"TestMain.java", "MockDbGenerator.java"}


def run(command, **kwargs):
    print("+ " + " ".join(str(arg) for arg in command), flush=True)
    return subprocess.run([str(arg) for arg in command], cwd=ROOT, check=True, **kwargs)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def host_target():
    system = {"Windows": "windows", "Linux": "linux", "Darwin": "macos"}.get(platform.system())
    arch = {"x86_64": "x64", "amd64": "x64", "aarch64": "arm64", "arm64": "arm64"}.get(platform.machine().lower())
    if not system or not arch:
        raise RuntimeError("Unsupported host OS or architecture: " + platform.platform())
    return system, arch


def find_jdk(requested):
    home = requested or os.environ.get("JAVA_HOME")
    if not home:
        javac = shutil.which("javac")
        if not javac:
            raise RuntimeError("Install JDK 21 and set JAVA_HOME, or pass --jdk-home.")
        home = Path(javac).resolve().parents[1]
    home = Path(home).resolve()
    suffix = ".exe" if os.name == "nt" else ""
    tools = {name: home / "bin" / (name + suffix) for name in ("java", "javac", "jar", "jlink", "jpackage")}
    for executable in tools.values():
        if not executable.is_file():
            raise FileNotFoundError(executable)
    version = run([tools["java"], "-version"], capture_output=True, text=True).stderr
    if not re.search(r'version "21[.\"]', version):
        raise RuntimeError("JDK 21 is required for release builds. Found: " + version)
    properties = run([tools["java"], "-XshowSettings:properties", "-version"], capture_output=True, text=True).stderr
    java_arch = re.search(r"^\s*os.arch\s*=\s*(\S+)", properties, re.MULTILINE)
    architectures = {"amd64": "x64", "x86_64": "x64", "aarch64": "arm64", "arm64": "arm64"}
    if not java_arch or architectures.get(java_arch.group(1).lower()) != host_target()[1]:
        raise RuntimeError("Use a native JDK matching the host CPU; emulated/cross-architecture packaging is not supported.")
    return tools, version.strip()


def dependencies():
    manifest = json.loads((ROOT / "scripts/dependencies.json").read_text(encoding="utf-8"))
    paths = []
    for entry in manifest["dependencies"]:
        path = ROOT / "lib" / entry["file"]
        if not path.is_file() or sha256(path) != entry["sha256"]:
            raise RuntimeError("Missing or modified dependency: " + str(path))
        paths.append(path)
    return paths


def isolated_environment(home):
    home.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    # JNI native launchers honour JAVA_TOOL_OPTIONS; quotes preserve spaces in paths.
    env["JAVA_TOOL_OPTIONS"] = '-Duser.home="' + str(home) + '" -Dfile.encoding=UTF-8'
    env.pop("JDK_JAVA_OPTIONS", None)
    env.pop("_JAVA_OPTIONS", None)
    env["XDG_DATA_HOME"] = str(home / ".local/share")
    return env


def run_tests(java, test_classes, app_classpath, work):
    names = sorted(path.stem for path in (ROOT / "tests").glob("*Test.java"))
    if not names:
        raise RuntimeError("--test requested but tests/*Test.java is empty")
    for name in names:
        run([java, "-ea", "-Djava.awt.headless=true", "-cp", str(test_classes) + os.pathsep + app_classpath, name],
            env=isolated_environment(work / ("home-test-" + name)), timeout=120)
    return names


def native_smoke(launcher, work, system):
    home = work / "home-native-smoke"
    env = isolated_environment(home)
    log = work / "native-smoke.log"
    with log.open("wb") as output:
        process = subprocess.Popen([str(launcher)], cwd=work, env=env, stdout=output, stderr=subprocess.STDOUT,
                                   start_new_session=(os.name != "nt"))
        try:
            deadline = time.monotonic() + 12
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError("Native launcher exited during startup: " + log.read_text(encoding="utf-8", errors="replace"))
                time.sleep(0.5)
            databases = list(home.rglob("ScoreData.db"))
            if len(databases) != 1:
                raise RuntimeError("Native launcher did not initialize an isolated database; see " + str(log))
            with sqlite3.connect(databases[0].as_uri() + "?mode=ro", uri=True) as database:
                if not database.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='users'").fetchone():
                    raise RuntimeError("Native startup did not create the accounts table")
            print("Native launcher startup passed; isolated database: " + str(databases[0]), flush=True)
        finally:
            if process.poll() is None:
                if system == "windows":
                    subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True)
                else:
                    import signal
                    os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=10)
    print(log.read_text(encoding="utf-8", errors="replace"), flush=True)


def git_info():
    try:
        commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
        dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip())
        return {"commit": commit, "working_tree_modified": dirty}
    except (OSError, subprocess.CalledProcessError):
        return {"commit": None, "working_tree_modified": None}


def copy_documentation(destination):
    for file in sorted(ROOT.glob("README*.md")):
        shutil.copy2(file, destination / file.name)
    for name in ("CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "LICENSE"):
        if (ROOT / name).is_file():
            shutil.copy2(ROOT / name, destination / name)
    for directory in ("docs", "examples"):
        source = ROOT / directory
        if source.is_dir():
            for file in source.rglob("*"):
                if file.is_file() and file.suffix.lower() in (".md", ".csv", ".html", ".png", ".svg"):
                    target = destination / file.relative_to(ROOT)
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(file, target)


def archive_release(package, system, output):
    if system == "macos":
        archive = output / (package.name + ".zip")
        # ditto preserves symlinks, executable bits, and bundle metadata on macOS.
        run(["/usr/bin/ditto", "-c", "-k", "--sequesterRsrc", "--keepParent", package, archive])
    elif system == "windows":
        archive = output / (package.name + ".zip")
        with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED, compresslevel=6) as target:
            for path in sorted(package.rglob("*")):
                if path.is_file():
                    target.write(path, path.relative_to(package.parent).as_posix())
    else:
        archive = output / (package.name + ".tar.gz")
        with tarfile.open(archive, "x:gz") as target:
            target.add(package, arcname=package.name)
    checksum = sha256(archive)
    (output / (archive.name + ".sha256")).write_text(checksum + "  " + archive.name + "\n", encoding="ascii")
    return archive, checksum


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default=os.environ.get("SCOREMANAGER_VERSION", "4.6.0"))
    parser.add_argument("--jdk-home", type=Path)
    parser.add_argument("--output-dir", type=Path, default=ROOT / "dist")
    parser.add_argument("--compile-only", action="store_true", help="Compile without creating a runtime or release")
    parser.add_argument("--test", action="store_true", help="Run tests/*Test.java on the JDK and packaged runtime")
    parser.add_argument("--smoke", action="store_true", help="Verify Swing startup and the native launcher in temporary homes; needs a desktop/Xvfb")
    args = parser.parse_args()
    version = args.version.removeprefix("v")
    if not re.fullmatch(r"[1-9][0-9]*\.[0-9]+\.[0-9]+", version):
        parser.error("version must be a release number such as 4.6.0 or v4.6.0")
    if args.compile_only and args.smoke:
        parser.error("--smoke requires a packaged runtime; omit --compile-only")
    system, arch = host_target()
    tools, java_version = find_jdk(args.jdk_home)
    libs = dependencies()
    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=True)
    build_parent = ROOT / "build"
    build_parent.mkdir(exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix=system + "-" + arch + "-", dir=build_parent))
    classes, input_dir, tests = (work / name for name in ("classes", "input", "test-classes"))
    for directory in (classes, input_dir, tests):
        directory.mkdir()
    sources = sorted(path for path in (ROOT / "src").rglob("*.java") if path.name not in DEVELOPMENT_SOURCES)
    lib_classpath = os.pathsep.join(str(path) for path in libs)
    run([tools["javac"], "-encoding", "UTF-8", "--release", "21", "-cp", lib_classpath, "-d", classes, *sources])
    app_classpath = str(classes) + os.pathsep + lib_classpath
    test_names = []
    if args.test:
        test_sources = sorted((ROOT / "tests").glob("*.java"))
        if not test_sources:
            raise RuntimeError("No test sources found")
        run([tools["javac"], "-encoding", "UTF-8", "--release", "21", "-cp", app_classpath, "-d", tests, *test_sources])
        test_names = run_tests(tools["java"], tests, app_classpath, work)
    if args.compile_only:
        print(json.dumps({"compiled_sources": len(sources), "tests_passed": test_names, "build_directory": str(work)}, indent=2))
        return
    for dependency in libs:
        shutil.copy2(dependency, input_dir / dependency.name)
    run([tools["jar"], "--create", "--file", input_dir / "ScoreManager.jar", "--main-class", "Main", "-C", classes, "."])
    with zipfile.ZipFile(input_dir / "ScoreManager.jar") as jar:
        if any(Path(name).stem.split("$")[0] in {"TestMain", "MockDbGenerator", "StartupSmoke"} for name in jar.namelist()):
            raise RuntimeError("Development helpers must not be packaged")
    runtime = work / "runtime"
    run([tools["jlink"], "--add-modules", MODULES, "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2", "--output", runtime])
    release_name = "ScoreManager-" + version + "-" + system + "-" + arch
    package = work / release_name
    package.mkdir()
    expected = output / (release_name + (".tar.gz" if system == "linux" else ".zip"))
    if expected.exists():
        raise FileExistsError("Refusing to overwrite existing release: " + str(expected))
    package_args = [tools["jpackage"], "--type", "app-image", "--name", "ScoreManager", "--app-version", version,
                    "--vendor", "ScoreManager", "--description", "Student score management / 学生积分管理",
                    "--input", input_dir, "--main-jar", "ScoreManager.jar", "--main-class", "Main",
                    "--runtime-image", runtime, "--dest", package, "--java-options", "-Dfile.encoding=UTF-8",
                    "--java-options", "-Dscoremanager.version=" + version]
    if system == "macos":
        package_args += ["--mac-package-identifier", "io.github.qihaolu8-maker.scoremanager",
                         "--mac-package-name", "ScoreManager", "--java-options", "-Dapple.awt.application.name=ScoreManager"]
    run(package_args)
    if system == "windows":
        image = package / "ScoreManager"
        launcher = image / "ScoreManager.exe"
        java = image / "runtime/bin/java.exe"
        packaged_libs = image / "app"
    elif system == "macos":
        image = package / "ScoreManager.app"
        launcher = image / "Contents/MacOS/ScoreManager"
        java = image / "Contents/runtime/Contents/Home/bin/java"
        packaged_libs = image / "Contents/app"
    else:
        image = package / "ScoreManager"
        launcher = image / "bin/ScoreManager"
        java = image / "lib/runtime/bin/java"
        packaged_libs = image / "lib/app"
        starter = package / "start-scoremanager.sh"
        starter.write_text('#!/bin/sh\nset -eu\nAPP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\nexec "$APP_DIR/ScoreManager/bin/ScoreManager" "$@"\n', encoding="utf-8")
        starter.chmod(0o755)
    if not launcher.is_file() or not java.is_file():
        raise RuntimeError("jpackage output is missing the native launcher or bundled Java")
    packaged_classpath = str(packaged_libs / "*")
    run([java, "-version"])
    if args.test:
        run_tests(java, tests, packaged_classpath, work / "packaged-tests")
    if args.smoke:
        run([tools["javac"], "-encoding", "UTF-8", "--release", "21", "-cp", app_classpath, "-d", tests, ROOT / "scripts/StartupSmoke.java"])
        run([java, "-cp", str(tests) + os.pathsep + packaged_classpath, "StartupSmoke"],
            env=isolated_environment(work / "home-swing-smoke"), timeout=60)
        native_smoke(launcher, work, system)
    copy_documentation(package)
    info = {"application": "ScoreManager", "version": version, "os": system, "architecture": arch,
            "built_at_utc": datetime.now(timezone.utc).isoformat(), "source": git_info(), "java": java_version,
            "runtime_modules": MODULES.split(","), "production_sources": [str(p.relative_to(ROOT)).replace("\\", "/") for p in sources],
            "dependencies_sha256": {p.name: sha256(p) for p in libs},
            "tests_passed_on_jdk_and_bundled_runtime": test_names,
            "swing_startup_smoke_passed": args.smoke, "native_launcher_startup_smoke_passed": args.smoke,
            "manual_full_workflow_tested": False, "publisher_signed": False, "apple_notarized": False}
    info_text = json.dumps(info, ensure_ascii=False, indent=2) + "\n"
    (package / "BUILD-INFO.json").write_text(info_text, encoding="utf-8")
    (output / (release_name + "-BUILD-INFO.json")).write_text(info_text, encoding="utf-8")
    archive, checksum = archive_release(package, system, output)
    print(json.dumps({"archive": str(archive), "sha256": checksum, "build_directory": str(work)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
