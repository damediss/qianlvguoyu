// schema-index 单测：纯函数契约（分组/确定性/差异），不连库
// 运行：node --test scripts/agent/schema-index.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { prefixOf, buildIndex, diffTables } from './schema-index.mjs';

const T = (over = {}) => ({ columns: [{ name: 'id', type: 'bigint', nullable: false, key: 'PRI', comment: '' }], indexes: [{ name: 'PRIMARY', unique: true, columns: ['id'] }], ...over });

test('prefixOf：按首个下划线前缀分组，无下划线归 misc', () => {
  assert.equal(prefixOf('train_exam_list'), 'train');
  assert.equal(prefixOf('exam_course'), 'exam');
  assert.equal(prefixOf('sys_user'), 'sys');
  assert.equal(prefixOf('auditlog'), 'misc');
});

test('buildIndex：表名排序 + 生成时间外确定性', () => {
  const tables = { 'exam_course': T(), 'train_exam_list': T(), 'exam_knowledge_structure': T() };
  const a = buildIndex('gjjddb2026', tables, '2026-10-20T00:00:00Z');
  const b = buildIndex('gjjddb2026', tables, '2026-10-21T00:00:00Z');
  assert.deepEqual(Object.keys(a), ['exam', 'train']);
  assert.deepEqual(Object.keys(a.exam.tables), ['exam_course', 'exam_knowledge_structure'], '表名须字典序');
  const strip = (o) => JSON.parse(JSON.stringify(o, (k, v) => (k === 'generatedAt' ? undefined : v)));
  assert.deepEqual(strip(a), strip(b));
  assert.equal(a.train._meta.notice.includes('禁止写入任何数据行'), true);
  assert.equal(a.train._meta.onCheckFail.includes('重新'), true);
});

test('diffTables：新增/缺失/结构变化三分类', () => {
  const oldT = { a: T(), b: T() };
  const newT = { a: T({ columns: T().columns.concat([{ name: 'x', type: 'int', nullable: true, key: '', comment: '' }]) }), c: T() };
  const d = diffTables(oldT, newT);
  assert.deepEqual(d.added, ['c']);
  assert.deepEqual(d.removed, ['b']);
  assert.equal(d.changed.length, 1);
  assert.match(d.changed[0], /a（列 1→2/);
});

test('diffTables：无差异返回空', () => {
  const d = diffTables({ a: T() }, { a: T() });
  assert.deepEqual(d, { added: [], removed: [], changed: [] });
});
