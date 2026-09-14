import test from 'node:test';
import assert from 'node:assert/strict';
import { parseArgs, applyScenario } from '../lib/args.mjs';
import { syntheticIdCard, syntheticMobile, idCardCheckCode } from '../lib/crypto.mjs';

test('parseArgs reads shard flags', () => {
  const options = parseArgs(['node', 'run.mjs', 'live', '--pads', '100', '--shard-index', '1', '--shard-count', '4']);
  assert.equal(options.command, 'live');
  assert.equal(options.pads, 100);
  assert.equal(options.shardIndex, 1);
  assert.equal(options.shardCount, 4);
});

test('smoke preset fills duration and questions', () => {
  const argv = ['node', 'run.mjs', 'live', '--scenario', 'smoke'];
  const options = applyScenario(parseArgs(argv), argv);
  assert.equal(options.pads, 10);
  assert.equal(options.questions, 8);
  assert.equal(options.durationSec, 300);
});

test('ws-gap-catchup preset keeps 10 pads and a disconnect window', () => {
  const argv = ['node', 'run.mjs', 'live', '--scenario', 'ws-gap-catchup'];
  const options = applyScenario(parseArgs(argv), argv);
  assert.equal(options.pads, 10);
  assert.equal(options.examDurationSec, 120);
  assert.ok(options.durationSec >= 70);
});

test('L4 preset overrides parseArgs numeric defaults', () => {
  const argv = ['node', 'run.mjs', 'live', '--scenario', 'L4'];
  const options = applyScenario(parseArgs(argv), argv);
  assert.equal(options.pads, 3000);
  assert.equal(options.consoles, 10);
  assert.equal(options.durationSec, 1800);
  assert.equal(options.questions, 100);
});

test('--pads flag wins over scenario preset', () => {
  const argv = ['node', 'run.mjs', 'live', '--scenario', 'L1', '--pads', '12'];
  const options = applyScenario(parseArgs(argv), argv);
  assert.equal(options.pads, 12);
  assert.equal(options.durationSec, 600);
});

test('synthetic identifiers are well-formed and not real PII', () => {
  const mobile = syntheticMobile(12);
  const idCard = syntheticIdCard(12);
  assert.match(mobile, /^199\d{8}$/);
  assert.equal(idCard.length, 18);
  assert.equal(idCard[17], idCardCheckCode(idCard.slice(0, 17)));
});
