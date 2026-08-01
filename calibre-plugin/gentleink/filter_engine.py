"""Shared GentleInk filter engine for Calibre (Python port)."""

from __future__ import annotations

import json
import pkgutil
import re
import unicodedata
import zipfile
from dataclasses import dataclass
from html import unescape
from pathlib import Path
from typing import Literal

Mode = Literal["remove", "mask", "substitute"]
Profile = Literal["family", "religious_strict"]

PLUGIN_PKG = "calibre_plugins.gentleink"
DATA_DIR = Path(__file__).resolve().parent / "core_data"
MAX_PASSES = 16
HTML_GAP = r"(?:<[^>]+>|&nbsp;|&#160;|\s)+"


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


def _normalize_input(text: str) -> str:
    text = unicodedata.normalize("NFKC", text)
    text = re.sub(r"[\u2018\u2019\u2032]", "'", text)
    text = re.sub(r"[\u201C\u201D]", '"', text)
    text = re.sub(r"&nbsp;|&#160;|&#xA0;", " ", text, flags=re.I)
    return text


def _apply_leet(text: str, leet_map: dict[str, str]) -> str:
    out = text
    for src, dst in leet_map.items():
        out = out.replace(src, dst)
    return out


def _sorted_word_list(tier1_words: set[str], ambiguous: dict) -> list[str]:
    tier1_sorted = sorted(tier1_words, key=len, reverse=True)
    ambiguous_sorted = sorted(
        (word for word in ambiguous if word not in tier1_words),
        key=len,
        reverse=True,
    )
    return tier1_sorted + ambiguous_sorted


def _dedupe_overlapping(matches: list["FilterMatch"]) -> list["FilterMatch"]:
    sorted_matches = sorted(matches, key=lambda m: (m.start, -(m.end - m.start)))
    kept: list[FilterMatch] = []
    for match in sorted_matches:
        if kept and match.start < kept[-1].end:
            if (match.end - match.start) > (kept[-1].end - kept[-1].start):
                kept[-1] = match
            continue
        kept.append(match)
    return kept


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
        self.leet_map = tier1.get("leetMap", {})
        self.ambiguous = context.get("ambiguous", {})
        self.phrases = context.get("phrases", [])
        self.tier1_safe = context.get("tier1Safe", {})
        self.substitutions = subs.get("profiles", {})
        self.word_list = _sorted_word_list(self.tier1_words, self.ambiguous)

    def prepare_text(self, text: str) -> str:
        return _apply_leet(_normalize_input(unescape(text)), self.leet_map)

    @staticmethod
    def _should_filter_context(rules: dict, safe: int, profane: int, profile: Profile) -> bool:
        if profile == "religious_strict":
            if safe > profane:
                return False
            if rules.get("defaultAction") == "skip" and safe == 0 and profane == 0:
                return False
            return True
        if profane > safe:
            return True
        if profane == safe and profane > 0:
            return rules.get("defaultAction") != "skip"
        if profane == 0 and safe == 0 and rules.get("defaultAction") == "context":
            return True
        return False

    @staticmethod
    def _build_phrase_regex(words: list[str], html_gap: str | None = None) -> str:
        gap = html_gap if html_gap is not None else r"\s+"
        return gap.join(re.escape(word) for word in words)

    def _is_tier1_safe(self, lemma: str, context_text: str) -> bool:
        patterns = self.tier1_safe.get(lemma, [])
        lower = context_text.lower()
        return any(re.search(pattern, lower, re.I) for pattern in patterns)

    def apply_phrases(
        self,
        text: str,
        profile: Profile = "family",
        mode: Mode = "substitute",
        mask_char: str = "*",
        html_gap: str | None = None,
    ) -> str:
        if mode == "remove" or not self.phrases:
            return text
        out = self.prepare_text(text)
        for phrase in self.phrases:
            replacement = phrase.get(profile) or phrase.get("family")
            if not replacement:
                continue
            pattern = re.compile(
                self._build_phrase_regex(phrase["words"], html_gap=html_gap),
                flags=re.IGNORECASE,
            )

            def replacer(match: re.Match, repl=replacement) -> str:
                original = match.group(0)
                if mode == "mask":
                    return mask_char * max(3, len(original))
                return self._preserve_case(original, repl)

            out = pattern.sub(replacer, out)
        return out

    def analyze(self, text: str, profile: Profile = "family", window_size: int = 120) -> list[FilterMatch]:
        prepared = self.prepare_text(text)
        matches: list[FilterMatch] = []

        for word in self.word_list:
            for match in re.finditer(rf"\b{re.escape(word)}\b", prepared, flags=re.IGNORECASE):
                start, end = match.span()
                token = text[start:end] if end <= len(text) else match.group(0)
                lemma = match.group(0).lower()

                if self._inside_compound(prepared, start, end):
                    continue
                if self._is_contraction(prepared, start, end):
                    continue

                ctx = prepared[max(0, start - window_size): min(len(prepared), end + window_size)]

                if lemma in self.tier1_words:
                    if self._is_tier1_safe(lemma, ctx):
                        continue
                    matches.append(FilterMatch(token, lemma, start, end, 1, "unambiguous profanity list"))
                    continue

                rules = self.ambiguous.get(lemma)
                if not rules:
                    continue

                safe = sum(1 for p in rules.get("safePatterns", []) if re.search(p, ctx, re.I))
                profane = sum(1 for p in rules.get("profanePatterns", []) if re.search(p, ctx, re.I))

                if self._should_filter_context(rules, safe, profane, profile):
                    if profane > safe:
                        reason = f"profane context ({profane} > {safe})"
                    elif profane == safe and profane > 0:
                        reason = "ambiguous context tie-breaker"
                    elif profile == "religious_strict":
                        reason = "strict profile default"
                    else:
                        reason = "expletive default (no safe context)"
                    matches.append(FilterMatch(token, lemma, start, end, 2, reason))

        return _dedupe_overlapping(matches)

    def filter_text_once(
        self,
        text: str,
        mode: Mode = "substitute",
        profile: Profile = "family",
        mask_char: str = "*",
    ) -> tuple[str, list[FilterMatch]]:
        prepared = self.apply_phrases(text, profile, mode, mask_char)
        matches = self.analyze(prepared, profile)
        if not matches:
            return prepared, []

        subs = self.substitutions.get(profile, {})
        out = prepared
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

    def filter_text(
        self,
        text: str,
        mode: Mode = "substitute",
        profile: Profile = "family",
        mask_char: str = "*",
        max_passes: int = MAX_PASSES,
    ) -> tuple[str, list[FilterMatch]]:
        out = text
        all_matches: list[FilterMatch] = []
        for _ in range(max_passes):
            before = out
            out, pass_matches = self.filter_text_once(out, mode, profile, mask_char)
            all_matches.extend(pass_matches)
            if out == before and not pass_matches:
                break
        return out, all_matches

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
