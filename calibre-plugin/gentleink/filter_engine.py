"""Shared GentleInk filter engine for Calibre (Python port)."""

from __future__ import annotations

import json
import pkgutil
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Literal

Mode = Literal["remove", "mask", "substitute"]
Profile = Literal["family", "religious_strict"]

PLUGIN_PKG = "calibre_plugins.gentleink"
DATA_DIR = Path(__file__).resolve().parent / "core_data"


def _plugin_zip_path() -> Path | None:
    for i, part in enumerate(Path(__file__).parts):
        if part.lower().endswith(".zip"):
            return Path(*Path(__file__).parts[: i + 1])
    return None


def _load_plugin_json(filename: str) -> dict:
    """Load bundled JSON from Calibre's zip plugin or local dev tree."""
    resource = f"core_data/{filename}"

    data = pkgutil.get_data(PLUGIN_PKG, resource)
    if data is not None:
        return json.loads(data.decode("utf-8"))

    zip_path = _plugin_zip_path()
    if zip_path is not None:
        with zipfile.ZipFile(zip_path, "r") as zf:
            for name in (resource, filename):
                try:
                    return json.loads(zf.read(name).decode("utf-8"))
                except KeyError:
                    continue

    base = Path(__file__).resolve().parent
    fs_path = base / "core_data" / filename
    try:
        with open(fs_path, encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, OSError):
        pass

    dev_path = base.parents[2] / "core" / "data" / filename
    if dev_path.is_file():
        with open(dev_path, encoding="utf-8") as f:
            return json.load(f)

    raise FileNotFoundError(
        f"GentleInk data file not found: {filename} "
        f"(checked {PLUGIN_PKG}:{resource}, plugin zip, {fs_path}, {dev_path})"
    )


@dataclass
class FilterMatch:
    word: str
    lemma: str
    start: int
    end: int
    tier: int
    reason: str


class GentleInkFilter:
    def __init__(self, data_dir: Path | None = None):
        if data_dir is None:
            allowlist = _load_plugin_json("allowlist.json")
            tier1 = _load_plugin_json("tier1-unambiguous.json")
            context = _load_plugin_json("context-rules.json")
            subs = _load_plugin_json("substitutions.json")
        else:
            allowlist = json.loads((data_dir / "allowlist.json").read_text(encoding="utf-8"))
            tier1 = json.loads((data_dir / "tier1-unambiguous.json").read_text(encoding="utf-8"))
            context = json.loads((data_dir / "context-rules.json").read_text(encoding="utf-8"))
            subs = json.loads((data_dir / "substitutions.json").read_text(encoding="utf-8"))

        self.compounds = {w.lower() for w in allowlist["compounds"]}
        self.contractions = {w.lower() for w in allowlist["contractions"]}
        self.tier1_words = {w.lower() for w in tier1["words"]}
        self.ambiguous = context.get("ambiguous", {})
        self.substitutions = subs.get("profiles", {})

    def analyze(self, text: str, profile: Profile = "family", window_size: int = 80) -> list[FilterMatch]:
        matches: list[FilterMatch] = []
        all_words = list(dict.fromkeys(list(self.tier1_words) + list(self.ambiguous.keys())))

        for word in all_words:
            for match in re.finditer(rf"\b{re.escape(word)}\b", text, flags=re.IGNORE_CASE):
                start, end = match.span()
                token = match.group(0)
                lemma = token.lower()

                if self._inside_compound(text, start, end):
                    continue
                if self._is_contraction(text, start, end):
                    continue

                if lemma in self.tier1_words:
                    matches.append(FilterMatch(token, lemma, start, end, 1, "unambiguous profanity list"))
                    continue

                rules = self.ambiguous.get(lemma)
                if not rules:
                    continue

                ctx = text[max(0, start - window_size): min(len(text), end + window_size)]
                safe = sum(1 for p in rules.get("safePatterns", []) if re.search(p, ctx, re.I))
                profane = sum(1 for p in rules.get("profanePatterns", []) if re.search(p, ctx, re.I))

                should_filter = profane > safe or (profane == safe > 0 and rules.get("defaultAction") != "skip")
                if should_filter:
                    reason = f"profane context ({profane} > {safe})" if profane > safe else "ambiguous context tie-breaker"
                    matches.append(FilterMatch(token, lemma, start, end, 2, reason))

        return sorted(matches, key=lambda m: m.start)

    def filter_text(
        self,
        text: str,
        mode: Mode = "substitute",
        profile: Profile = "family",
        mask_char: str = "*",
    ) -> tuple[str, list[FilterMatch]]:
        matches = self.analyze(text, profile)
        if not matches:
            return text, []

        subs = self.substitutions.get(profile, {})
        out = text
        offset = 0
        for m in matches:
            start = m.start + offset
            end = m.end + offset
            original = out[start:end]
            if mode == "remove":
                replacement = ""
            elif mode == "mask":
                replacement = mask_char * max(3, len(original))
            else:
                replacement = self._preserve_case(original, subs.get(m.lemma, mask_char * 3))
            out = out[:start] + replacement + out[end:]
            offset += len(replacement) - len(original)
        return out, matches

    def _inside_compound(self, text: str, start: int, end: int) -> bool:
        lower = text.lower()
        for compound in self.compounds:
            idx = 0
            while True:
                idx = lower.find(compound, idx)
                if idx < 0:
                    break
                if start >= idx and end <= idx + len(compound):
                    return True
                idx += 1
        return False

    def _is_contraction(self, text: str, start: int, end: int) -> bool:
        span = text[start:end].lower()
        expanded = text[max(0, start - 2): min(len(text), end + 2)].lower()
        return any(span == c or c in expanded for c in self.contractions)

    @staticmethod
    def _preserve_case(original: str, replacement: str) -> str:
        if not original:
            return replacement
        if not replacement:
            return original
        if original.isupper():
            return replacement.upper()
        if original[0].isupper():
            return replacement[:1].upper() + replacement[1:]
        return replacement
