// schema-find 单测：fixture 输出契约 + 真实仓库事故回归（离线，不连库）
// 运行：node --test scripts/agent/schema-find.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { collectCandidates, searchRepo, formatText, main } from './schema-find.mjs';

const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

function makeFixture() {
  const root = mkdtempSync(join(tmpdir(), 'schema-find-'));
  const migDir = join(root, 'services/platform-api/src/main/resources/db/migration');
  const entDir = join(root, 'services/platform-api/src/main/java/com/x/entity');
  const svcDir = join(root, 'services/platform-api/src/main/java/com/x/service');
  for (const d of [migDir, entDir, svcDir]) mkdirSync(d, { recursive: true });
  writeFileSync(join(migDir, 'V1__t.sql'), [
    'CREATE TABLE train_exam_work_type_list (',
    '  id BIGINT PRIMARY KEY,',
    '  work_type_id BIGINT NULL COMMENT \'工种ID\'',
    ');',
  ].join('\n'));
  writeFileSync(join(entDir, 'TrainExamWorkTypeListEntity.java'), 'public class TrainExamWorkTypeListEntity {\n  private Long workTypeId;\n}\n');
  writeFileSync(join(svcDir, 'FooService.java'), 'public class FooService {\n  Long examId; // 反向引用\n}\n');
  return root;
}

test('候选键：蛇形/驼峰互转 + train_/exam_ 前缀尾词', () => {
  const keys = collectCandidates('train_exam_list');
  assert.ok(keys.includes('examId') && keys.includes('exam_list'), `尾词候选缺失: ${keys}`);
  assert.ok(collectCandidates('work_type_id').includes('workTypeId'));
});

test('fixture：迁移/实体/引用三段命中与输出顺序', () => {
  const root = makeFixture();
  try {
    const hits = searchRepo(root, 'work_type_id');
    assert.equal(hits.migrations.length, 1);
    assert.match(hits.migrations[0].file, /V1__t\.sql$/);
    assert.equal(hits.entities.length, 1);
    assert.match(hits.entities[0].file, /TrainExamWorkTypeListEntity\.java$/);
    const text = formatText('work_type_id', hits, null, 40);
    const mi = text.indexOf('[migrations]');
    const ei = text.indexOf('[entities]');
    const ri = text.indexOf('[references]');
    assert.ok(mi !== -1 && ei !== -1 && ri !== -1 && mi < ei && ei < ri, '分段顺序应为 migrations→entities→references');
    assert.match(text, /V1__t\.sql:3\s+work_type_id/, '命中必须带 文件:行号');
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('fixture：超出行数预算折叠为“还有 N 条”', () => {
  const root = makeFixture();
  try {
    const hits = searchRepo(root, 'work_type_id');
    hits.references = Array.from({ length: 5 }, (_, i) => ({ file: `R${i}.java`, line: i + 1, text: 'x' }));
    const text = formatText('work_type_id', hits, null, 3);
    assert.match(text, /还有 \d+ 条，--limit 查看/);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('分段保底：宽关键词下 db 段仍展示 3 行，不被迁移段吃光预算', () => {
  const mk = (n, p) => Array.from({ length: n }, (_, i) => ({ file: `${p}${i}.sql`, line: i + 1, text: 'x' }));
  const hits = { migrations: mk(100, 'M'), entities: mk(5, 'E'), references: mk(5, 'R') };
  const db = { db: Array.from({ length: 10 }, (_, i) => `t${i}.c${i} bigint NULL`) };
  const text = formatText('level', hits, db, 20);
  const dbShown = text.split('\n').filter((l) => l.startsWith('  t')).length;
  assert.equal(dbShown, 3, `db 段应保底 3 行，实际 ${dbShown}`);
  assert.match(text, /还有 /);
});

test('事故回归：work_type_id 命中关联表与实体（真实仓库，离线）', () => {
  const hits = searchRepo(repoRoot, 'work_type_id');
  // 命中行是列定义行（不含表名），归因到建表文件即等价于"命中 train_exam_work_type_list"
  const mig = hits.migrations.find((h) => h.file.includes('V20260521_001') && /work_type_id/.test(h.text));
  assert.ok(mig, '迁移必须命中 train_exam_work_type_list 的建表文件（V20260521_001）');
  const entFiles = hits.entities.map((h) => h.file).join('\n');
  assert.match(entFiles, /TrainExamWorkTypeListEntity\.java/);
});

test('同名不同义回归：level 同时命中 train_exam_list 与 train_exam_work_type_list（真实仓库，离线）', () => {
  const hits = searchRepo(repoRoot, 'level');
  const migText = hits.migrations.map((h) => h.text).join('\n');
  // 离线模式的归因代理：两张表各自的 level 列定义行必须都出现；
  // 精确的 table.column 归因由 --db test 的 information_schema 段完成
  assert.match(migText, /level VARCHAR\(100\)/, 'train_exam_list.level（V20260519_005）未命中');
  assert.match(migText, /level VARCHAR\(50\)/, 'train_exam_work_type_list.level（V20260521_001）未命中');
});

test('main：缺关键词退出码 2', () => {
  const originalExit = process.exit;
  const originalErr = console.error;
  let code = null;
  process.exit = (c) => {
    code = c;
    throw new Error('__exit__');
  };
  console.error = () => {};
  try {
    assert.throws(() => main([]), /__exit__/);
    assert.equal(code, 2);
  } finally {
    process.exit = originalExit;
    console.error = originalErr;
  }
});
