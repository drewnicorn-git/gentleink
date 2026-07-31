#!/usr/bin/env python3
"""Smoke test for GentleInk Python filter engine."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from gentleink.filter_engine import GentleInkFilter

filter = GentleInkFilter()
text = "What the hell! Move your ass or I will kick your ass."
filtered, matches = filter.filter_text(text, mode="substitute", profile="family")
print("Input: ", text)
print("Output:", filtered)
print("Matches:", [f"{m.word} ({m.reason})" for m in matches])
