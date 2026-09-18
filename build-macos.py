#!/usr/bin/env python3
"""Compatibility entry point. macOS apps must now be built natively on macOS."""
import platform
import runpy
from pathlib import Path

if platform.system() != "Darwin":
    raise SystemExit("Build macOS apps on a Mac, or run the GitHub Actions 'Build native releases' workflow. See docs/BUILD.md.")

runpy.run_path(str(Path(__file__).resolve().parent / "scripts/build.py"), run_name="__main__")
