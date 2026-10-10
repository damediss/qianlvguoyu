import test from 'node:test';
import assert from 'node:assert/strict';
import { metricCount, nodeRequestShare } from '../lib/collector.mjs';

test('metric COUNT prefers the COUNT statistic', () => {
  assert.equal(metricCount({
    measurements: [
      { statistic: 'VALUE', value: 3 },
      { statistic: 'COUNT', value: 41 },
    ],
  }), 41);
  assert.equal(metricCount({ measurements: [] }), 0);
});

test('node request share is balanced only in the 40-60 band', () => {
  const start = [
    { node: 'api-1', status: 200, count: 100 },
    { node: 'api-2', status: 200, count: 80 },
  ];
  const balanced = nodeRequestShare(start, [
    { node: 'api-1', status: 200, count: 150 },
    { node: 'api-2', status: 200, count: 125 },
  ]);
  assert.equal(balanced.total, 95);
  assert.equal(balanced.balanced, true);
  assert.ok(balanced.nodes[0].share > 0.4);
  assert.ok(balanced.nodes[0].share < 0.6);

  const skewed = nodeRequestShare(start, [
    { node: 'api-1', status: 200, count: 190 },
    { node: 'api-2', status: 200, count: 85 },
  ]);
  assert.equal(skewed.balanced, false);
  assert.ok(skewed.nodes[0].share > 0.9);
});
