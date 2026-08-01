import fs from 'node:fs';
import path from 'node:path';

const MODES = ['remove', 'mask', 'substitute'];
const PROFILES = ['family', 'religious_strict'];

function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function normalizeLeet(text, leetMap) {
  let out = text.toLowerCase();
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

export class GentleInkFilter {
  constructor({ allowlist, tier1, contextRules, substitutions }) {
    this.compounds = allowlist.compounds.map((w) => w.toLowerCase());
    this.contractions = allowlist.contractions.map((w) => w.toLowerCase());
    this.tier1Words = tier1.words.map((w) => w.toLowerCase());
    this.leetMap = tier1.leetMap ?? {};
    this.ambiguous = contextRules.ambiguous ?? {};
    this.substitutions = substitutions.profiles ?? {};
  }

  static fromDataDir(dataDir) {
    return new GentleInkFilter({
      allowlist: loadJson(path.join(dataDir, 'allowlist.json')),
      tier1: loadJson(path.join(dataDir, 'tier1-unambiguous.json')),
      contextRules: loadJson(path.join(dataDir, 'context-rules.json')),
      substitutions: loadJson(path.join(dataDir, 'substitutions.json')),
    });
  }

  analyze(text, { profile = 'family', windowSize = 80 } = {}) {
    const matches = [];
    const lower = text.toLowerCase();
    const allWords = new Set([
      ...this.tier1Words,
      ...Object.keys(this.ambiguous),
    ]);

    for (const word of allWords) {
      const pattern = new RegExp(`\\b${word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'gi');
      let match;
      while ((match = pattern.exec(text)) !== null) {
        const start = match.index;
        const end = start + match[0].length;
        if (!isWordBoundary(text, start, end)) continue;
        if (isInsideCompound(text, start, end, this.compounds)) continue;
        if (isContractionSpan(text, start, end, this.contractions)) continue;

        const token = match[0];
        const lemma = token.toLowerCase();
        const contextStart = Math.max(0, start - windowSize);
        const contextEnd = Math.min(text.length, end + windowSize);
        const contextText = text.slice(contextStart, contextEnd);

        if (this.tier1Words.includes(lemma)) {
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
        let shouldFilter = false;
        let reason = '';

        if (profane > safe) {
          shouldFilter = true;
          reason = `profane context (${profane} > ${safe})`;
        } else if (profane === safe && profane > 0) {
          shouldFilter = rules.defaultAction !== 'skip';
          reason = 'ambiguous context tie-breaker';
        } else if (profane === 0 && safe === 0 && rules.defaultAction === 'context') {
          shouldFilter = false;
          reason = 'no context signal, skipped';
        }

        if (shouldFilter) {
          matches.push({ word: token, lemma, start, end, tier: 2, reason });
        }
      }
    }

    return matches.sort((a, b) => a.start - b.start);
  }

  filterText(text, { mode = 'substitute', profile = 'family', maskChar = '*' } = {}) {
    if (!MODES.includes(mode)) throw new Error(`Unknown mode: ${mode}`);
    if (!PROFILES.includes(profile)) throw new Error(`Unknown profile: ${profile}`);

    const matches = this.analyze(text, { profile });
    if (matches.length === 0) {
      return { text, matches: [], changed: false };
    }

    const subs = this.substitutions[profile] ?? {};
    let out = text;
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
}

export { MODES, PROFILES, normalizeLeet };
