import fs from 'node:fs';
import path from 'node:path';

const MODES = ['remove', 'mask', 'substitute'];
const PROFILES = ['family', 'religious_strict'];
const MAX_PASSES = 16;

function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function normalizeInput(text) {
  return text
    .normalize('NFKC')
    .replace(/[\u2018\u2019\u2032]/g, "'")
    .replace(/[\u201C\u201D]/g, '"')
    .replace(/&nbsp;|&#160;|&#xA0;/gi, ' ');
}

function applyLeetNormalization(text, leetMap) {
  let out = text;
  for (const [from, to] of Object.entries(leetMap)) {
    out = out.split(from).join(to);
  }
  return out;
}

function isWordBoundary(text, start, end) {
  const before = start > 0 ? text[start - 1] : ' ';
  const after = end < text.length ? text[end] : ' ';
  return !/[a-z0-9']/i.test(before) && !/[a-z0-9']/i.test(after);
}

function isInsideCompound(text, start, end, compounds) {
  const lower = text.toLowerCase();
  for (const compound of compounds) {
    let idx = 0;
    while ((idx = lower.indexOf(compound, idx)) !== -1) {
      const cEnd = idx + compound.length;
      if (start >= idx && end <= cEnd) return true;
      idx += 1;
    }
  }
  return false;
}

function isContractionSpan(text, start, end, contractions) {
  const span = text.slice(start, end).toLowerCase();
  const expanded = text.slice(Math.max(0, start - 2), Math.min(text.length, end + 2)).toLowerCase();
  return contractions.some((c) => span === c || expanded.includes(c));
}

function preserveCase(original, replacement) {
  if (!original) return replacement;
  if (!replacement) return original;
  if (original === original.toUpperCase()) return replacement.toUpperCase();
  if (original[0] === original[0].toUpperCase()) {
    return replacement.charAt(0).toUpperCase() + replacement.slice(1);
  }
  return replacement;
}

function scoreContext(text, rules) {
  const lower = text.toLowerCase();
  let safe = 0;
  let profane = 0;
  for (const pattern of rules.safePatterns ?? []) {
    if (new RegExp(pattern, 'i').test(lower)) safe += 1;
  }
  for (const pattern of rules.profanePatterns ?? []) {
    if (new RegExp(pattern, 'i').test(lower)) profane += 1;
  }
  return { safe, profane };
}

function shouldFilterContext(rules, safe, profane, profile) {
  if (profile === 'religious_strict') {
    if (safe > profane) return false;
    if (rules.defaultAction === 'skip' && safe === 0 && profane === 0) return false;
    return true;
  }
  if (profane > safe) return true;
  if (profane === safe && profane > 0) return rules.defaultAction !== 'skip';
  if (profane === 0 && safe === 0 && rules.defaultAction === 'context') return true;
  return false;
}

function buildPhraseRegex(words, htmlGap = null) {
  const gap = htmlGap ?? '\\s+';
  return words
    .map((word) => word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    .join(gap);
}

function dedupeOverlapping(matches) {
  const sorted = [...matches].sort(
    (a, b) => a.start - b.start || (b.end - b.start) - (a.end - a.start),
  );
  const kept = [];
  for (const match of sorted) {
    const last = kept[kept.length - 1];
    if (last && match.start < last.end) {
      if (match.end - match.start > last.end - last.start) {
        kept[kept.length - 1] = match;
      }
      continue;
    }
    kept.push(match);
  }
  return kept;
}

function sortedWordList(tier1Words, ambiguous) {
  const tier1Sorted = [...tier1Words].sort((a, b) => b.length - a.length);
  const ambiguousSorted = Object.keys(ambiguous)
    .filter((word) => !tier1Sorted.includes(word))
    .sort((a, b) => b.length - a.length);
  return [...tier1Sorted, ...ambiguousSorted];
}

export class GentleInkFilter {
  constructor({ allowlist, tier1, contextRules, substitutions }) {
    this.compounds = allowlist.compounds.map((w) => w.toLowerCase());
    this.contractions = allowlist.contractions.map((w) => w.toLowerCase());
    this.tier1Words = tier1.words.map((w) => w.toLowerCase());
    this.leetMap = tier1.leetMap ?? {};
    this.ambiguous = contextRules.ambiguous ?? {};
    this.phrases = contextRules.phrases ?? [];
    this.tier1Safe = contextRules.tier1Safe ?? {};
    this.substitutions = substitutions.profiles ?? {};
    this.wordList = sortedWordList(this.tier1Words, this.ambiguous);
  }

  static fromDataDir(dataDir) {
    return new GentleInkFilter({
      allowlist: loadJson(path.join(dataDir, 'allowlist.json')),
      tier1: loadJson(path.join(dataDir, 'tier1-unambiguous.json')),
      contextRules: loadJson(path.join(dataDir, 'context-rules.json')),
      substitutions: loadJson(path.join(dataDir, 'substitutions.json')),
    });
  }

  prepareText(text) {
    return applyLeetNormalization(normalizeInput(text), this.leetMap);
  }

  isTier1Safe(lemma, contextText) {
    const patterns = this.tier1Safe[lemma] ?? [];
    const lower = contextText.toLowerCase();
    return patterns.some((pattern) => new RegExp(pattern, 'i').test(lower));
  }

  analyze(text, { profile = 'family', windowSize = 120 } = {}) {
    const prepared = this.prepareText(text);
    const matches = [];

    for (const word of this.wordList) {
      const pattern = new RegExp(`\\b${word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'gi');
      let match;
      while ((match = pattern.exec(prepared)) !== null) {
        const start = match.index;
        const end = start + match[0].length;
        if (!isWordBoundary(prepared, start, end)) continue;
        if (isInsideCompound(prepared, start, end, this.compounds)) continue;
        if (isContractionSpan(prepared, start, end, this.contractions)) continue;

        const token = text.slice(start, end);
        const lemma = match[0].toLowerCase();
        const contextStart = Math.max(0, start - windowSize);
        const contextEnd = Math.min(text.length, end + windowSize);
        const contextText = prepared.slice(contextStart, contextEnd);

        if (this.tier1Words.includes(lemma)) {
          if (this.isTier1Safe(lemma, contextText)) continue;
          matches.push({
            word: token,
            lemma,
            start,
            end,
            tier: 1,
            reason: 'unambiguous profanity list',
          });
          continue;
        }

        const rules = this.ambiguous[lemma];
        if (!rules) continue;

        const { safe, profane } = scoreContext(contextText, rules);
        if (shouldFilterContext(rules, safe, profane, profile)) {
          const reason = profane > safe
            ? `profane context (${profane} > ${safe})`
            : profane === safe && profane > 0
              ? 'ambiguous context tie-breaker'
              : profile === 'religious_strict'
                ? 'strict profile default'
                : 'expletive default (no safe context)';
          matches.push({ word: token, lemma, start, end, tier: 2, reason });
        }
      }
    }

    return dedupeOverlapping(matches);
  }

  applyPhrases(text, { profile = 'family', htmlGap = null, mode = 'substitute', maskChar = '*' } = {}) {
    if (mode === 'remove' || !this.phrases.length) return text;
    let out = this.prepareText(text);
    for (const phrase of this.phrases) {
      const replacement = phrase[profile] ?? phrase.family;
      if (!replacement) continue;
      const pattern = new RegExp(buildPhraseRegex(phrase.words, htmlGap), 'gi');
      out = out.replace(pattern, (match) => {
        if (mode === 'mask') return maskChar.repeat(Math.max(3, match.length));
        return preserveCase(match, replacement);
      });
    }
    return out;
  }

  filterTextOnce(text, { mode = 'substitute', profile = 'family', maskChar = '*' } = {}) {
    const prepared = this.applyPhrases(text, { profile, mode, maskChar });
    const matches = this.analyze(prepared, { profile });
    if (matches.length === 0) {
      return { text: prepared, matches: [], changed: prepared !== text };
    }

    const subs = this.substitutions[profile] ?? {};
    let out = prepared;
    let offset = 0;

    for (const match of matches) {
      const start = match.start + offset;
      const end = match.end + offset;
      const original = out.slice(start, end);
      const lemma = match.lemma.toLowerCase();

      let replacement = '';
      if (mode === 'remove') {
        replacement = '';
      } else if (mode === 'mask') {
        replacement = maskChar.repeat(Math.max(3, original.length));
      } else {
        replacement = subs[lemma] ?? subs[original.toLowerCase()] ?? maskChar.repeat(3);
        replacement = preserveCase(original, replacement);
      }

      out = out.slice(0, start) + replacement + out.slice(end);
      offset += replacement.length - original.length;
    }

    return { text: out, matches, changed: true };
  }

  filterText(text, { mode = 'substitute', profile = 'family', maskChar = '*', maxPasses = MAX_PASSES } = {}) {
    if (!MODES.includes(mode)) throw new Error(`Unknown mode: ${mode}`);
    if (!PROFILES.includes(profile)) throw new Error(`Unknown profile: ${profile}`);

    let out = text;
    const allMatches = [];

    for (let pass = 0; pass < maxPasses; pass += 1) {
      const before = out;
      const result = this.filterTextOnce(out, { mode, profile, maskChar });
      out = result.text;
      allMatches.push(...result.matches);
      if (out === before && !result.changed) break;
    }

    return { text: out, matches: allMatches, changed: out !== text };
  }
}

export { MODES, PROFILES, buildPhraseRegex, MAX_PASSES, normalizeInput };
