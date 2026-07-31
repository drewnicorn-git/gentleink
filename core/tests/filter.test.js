import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GentleInkFilter } from '../src/filter.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dataDir = path.join(__dirname, '..', 'data');
const goldenPath = path.join(dataDir, 'golden-tests.json');

const filter = GentleInkFilter.fromDataDir(dataDir);
const tests = JSON.parse(fs.readFileSync(goldenPath, 'utf8'));

let passed = 0;
let failed = 0;

for (const test of tests) {
  const mode = test.mode ?? 'substitute';
  const profile = test.profile ?? 'family';
  const result = filter.filterText(test.input, { mode, profile });
  const matches = filter.analyze(test.input, { profile });

  let ok = true;
  let reason = '';

  if (test.shouldFilter === false) {
    if (matches.length > 0) {
      ok = false;
      reason = `expected no matches, got: ${matches.map((m) => m.word).join(', ')}`;
    }
  } else if (test.shouldFilter === true && !test.mode) {
    if (matches.length === 0) {
      ok = false;
      reason = 'expected at least one match';
    } else if (test.lemma && !matches.some((m) => m.lemma === test.lemma || m.word === test.lemma)) {
      ok = false;
      reason = `expected lemma ${test.lemma}, got ${matches.map((m) => m.lemma ?? m.word).join(', ')}`;
    }
  }

  if (test.expectedContains && !result.text.includes(test.expectedContains)) {
    ok = false;
    reason = `expected output to contain "${test.expectedContains}", got: ${result.text}`;
  }

  if (test.expectedNotContains && result.text.toLowerCase().includes(test.expectedNotContains)) {
    ok = false;
    reason = `expected output to omit "${test.expectedNotContains}", got: ${result.text}`;
  }

  if (test.expectedPattern && !new RegExp(test.expectedPattern).test(result.text)) {
    ok = false;
    reason = `expected pattern ${test.expectedPattern}, got: ${result.text}`;
  }

  if (ok) {
    passed++;
  } else {
    failed++;
    console.error(`FAIL [${test.id}]: ${reason}`);
    console.error(`  input:  ${test.input}`);
    console.error(`  output: ${result.text}`);
  }
}

console.log(`Golden tests: ${passed} passed, ${failed} failed, ${tests.length} total`);
if (failed > 0) process.exit(1);
