#!/usr/bin/env python3
"""Rebuild nav_dua only — see build-nav-glyph-icons.py for shared pipeline."""
import subprocess
import sys
from pathlib import Path

if __name__ == "__main__":
    script = Path(__file__).resolve().parent / "build-nav-glyph-icons.py"
    raise SystemExit(subprocess.call([sys.executable, str(script)]))
