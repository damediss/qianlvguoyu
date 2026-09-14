import test from 'node:test';
import assert from 'node:assert/strict';
import { ticketUrl, wsHttpToWs, parseEnvelope } from '../lib/ws.mjs';

test('ws url uses host of API and ticket query', () => {
  const url = ticketUrl('http://127.0.0.1:18080/api', '/api/ws/competition/formal-exam/player', 'abc+def');
  assert.equal(url.startsWith('ws://127.0.0.1:18080/api/ws/competition/formal-exam/player?ticket='), true);
  assert.equal(url.includes('abc'), true);
  assert.equal(wsHttpToWs('https://exam.local/api', '/api/ws/x').startsWith('wss://'), true);
});

test('parse envelope rejects non json', () => {
  assert.equal(parseEnvelope('not-json'), null);
  assert.equal(parseEnvelope('{"type":"application.heartbeat"}').type, 'application.heartbeat');
});
