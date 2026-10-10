import test from 'node:test';
import assert from 'node:assert/strict';
import { evaluateIsolation, schemaName, hostName, containsBlockedFragment } from '../lib/gate.mjs';

test('gate accepts isolated load schema with confirmations', () => {
  const result = evaluateIsolation({
    jdbcUrl: 'jdbc:mysql://127.0.0.1:13306/gjjddb_load',
    jdbcSchema: 'gjjddb_load',
    jdbcHost: '127.0.0.1',
    profiles: 'loadtest',
    pads: 100,
    writeAnswers: true,
    isolatedInfra: true,
    env: { FORMAL_EXAM_LOAD_ALLOW_LARGE: '1', FORMAL_EXAM_LOAD_ISOLATED: 'I_UNDERSTAND' },
  });
  assert.equal(result.ok, true);
  assert.equal(result.jdbcSchema, 'gjjddb_load');
});

test('gate blocks shared host and shared schema', () => {
  assert.equal(containsBlockedFragment('jdbc:mysql://43.138.163.7:3308/gjjddb_load'), true);
  const shared = evaluateIsolation({
    jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/gjjddb2026',
    profiles: 'loadtest',
    pads: 1,
    writeAnswers: false,
    isolatedInfra: true,
    env: {},
  });
  assert.equal(shared.ok, false);
});

test('gate requires dual confirmation for writes and large pads', () => {
  const result = evaluateIsolation({
    jdbcUrl: 'jdbc:mysql://127.0.0.1:13306/gjjddb_load',
    jdbcSchema: 'gjjddb_load',
    profiles: 'loadtest',
    pads: 11,
    writeAnswers: false,
    isolatedInfra: true,
    env: {},
  });
  assert.equal(result.ok, false);
  assert.ok(result.reasons.some((reason) => reason.includes('ALLOW_LARGE')));
});

test('schema and host parsers', () => {
  assert.equal(schemaName('jdbc:mysql://fel-mysql:3306/gjjddb_load?useSSL=false'), 'gjjddb_load');
  assert.equal(hostName('jdbc:mysql://127.0.0.1:13306/gjjddb_load'), '127.0.0.1');
});
