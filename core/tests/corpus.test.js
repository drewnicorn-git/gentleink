import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GentleInkFilter } from '../src/filter.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dataDir = path.join(__dirname, '..', 'data');
const corpusPath = path.join(dataDir, 'profanity-corpus.json');

const filter = GentleInkFilter.fromDataDir(dataDir);
const corpus = JSON.parse(fs.readFileSync(corpusPath, 'utf8'));

let passed = 0;
let failed = 0;

for (const [index, item] of corpus.mustFilter.entries()) {
  for (const profile of ['family', 'religious_strict']) {
    const result = filter.filterText(item.text, { mode: 'substitute', profile });
    const hit = item.forbidden.some((word) => result.text.includes(word));
    if (hit) {
      failed += 1;
      console.error(`FAIL corpus filter [${index}] profile=${profile}`);
      console.error(`  input:  ${item.text}`);
      console.error(`  output: ${result.text}`);
    } else {
      passed += 1;
    }
  }
}

for (const [index, item] of corpus.mustPreserve.entries()) {
  for (const profile of ['family', 'religious_strict']) {
    const matches = filter.analyze(item.text, { profile });
    if (matches.length > 0) {
      failed += 1;
      console.error(`FAIL corpus preserve [${index}] profile=${profile}`);
      console.error(`  input:   ${item.text}`);
      console.error(`  matched: ${matches.map((m) => m.word).join(', ')}`);
    } else {
      passed += 1;
    }
  }
}

console.log(`Corpus tests: ${passed} passed, ${failed} failed, ${passed + failed} total`);
if (failed > 0) process.exit(1);
