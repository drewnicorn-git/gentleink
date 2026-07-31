#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GentleInkFilter } from './filter.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dataDir = path.join(__dirname, '..', 'data');
const filter = GentleInkFilter.fromDataDir(dataDir);

const text = process.argv.slice(2).join(' ') || 'What the hell! Move your ass or I will kick your ass.';
const result = filter.filterText(text, { mode: 'substitute', profile: 'family' });

console.log('Input: ', text);
console.log('Output:', result.text);
console.log('Matches:', result.matches.map((m) => `${m.word} (${m.reason})`).join('; ') || 'none');
