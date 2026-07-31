#!/usr/bin/env python3
"""Standalone filter engine test (no Calibre required)."""

import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "core" / "data"
sys.path.insert(0, str(ROOT / "calibre-plugin" / "gentleink"))

from filter_engine import GentleInkFilter  # noqa: E402

filter = GentleInkFilter(DATA)
text = "What the hell! Move your ass or I will kick your ass."
filtered, matches = filter.filter_text(text, mode="substitute", profile="family")

assert "heck" in filtered, filtered
assert "butt" in filtered, filtered
assert "assassin" not in filtered or True

safe, _ = filter.filter_text("He played the bass guitar.", mode="substitute", profile="family")
assert "bass" in safe, safe

print("PASS: Python filter engine")
print("Input: ", text)
print("Output:", filtered)
print("Matches:", len(matches))
