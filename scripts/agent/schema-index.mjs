#!/usr/bin/env node
// schema-index.mjs —— 生成/校验 docs/agent/schema-index.<前缀>.json（表/列/索引结构索引，不含数据）
// 用法：node scripts/agent/schema-index.mjs --generate|--check [--db test|prod] [--root DIR]
// 退出码：0 生成成功 / --check 一致；1 --check 发现漂移或生成物损坏；2 参数错误；3 依赖缺失（不修改已提交文件）
// 口径：--check 失败的唯一处置 = 以测试库为准重新 --generate 提交（结构变化只能来自迁移）。
// 互指：迁移版本/双写对基线见 docs/agent/项目现状.md（gen-agents-context.mjs 生成）。

import { readdirSync, readFileSync, writeFileSync, existsSync, unlinkSync } from 'node:fs';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { resolveDbConfig, resolveMysqlBin } from './schema-find.mjs';

const NOTICE = '仅含结构元数据，禁止写入任何数据行；迁移版本/双写对基线见 docs/agent/项目现状.md';
const ON_CHECK_FAIL = '--check 失败一律以测试库为准重新 --generate 提交（结构变化只能来自迁移）';
const OUT_DIR = join('docs', 'agent');

export function prefixOf(table) {
  const i = table.indexOf('_');
  return i <= 0 ? 'misc' : table.slice(0, i);
}

function runQuery(root, cfg, bin, sql) {
  const r = spawnSync(bin, [
    '-h', cfg.host, '-P', String(cfg.port), '-u', cfg.user,
    `--database=${cfg.database}`, '--default-character-set=utf8mb4',
    '--batch', '--skip-column-names', '--connect-timeout=5', '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: cfg.password }, encoding: 'utf8', timeout: 15000, maxBuffer: 32 * 1024 * 1024 });
  if (r.error) {
    const missing = r.error.code === 'ENOENT';
    return { error: missing ? `找不到 mysql 客户端（${bin}）：请安装、加入 PATH 或 export MYSQL_BIN` : `mysql 调用失败：${r.error.message}` };
  }
  if (r.status !== 0) return { error: `mysql 错误（exit ${r.status}）：${(r.stderr || '').split('\n')[0]}` };
  return { rows: (r.stdout || '').split('\n').filter((l) => l.trim()).map((l) => l.split('\t')) };
}

// 拉取全库结构：tables[t] = { columns: [...按 ORDINAL_POSITION], indexes: [...按 SEQ_IN_INDEX] }
export function fetchSchema(root, profile) {
  const cfg = resolveDbConfig(root, profile);
  if (!cfg || !cfg.password) return { error: `缺少连接信息：请 export MYSQL_HOST/MYSQL_USER/MYSQL_PWD（${profile}），或在仓库根提供 AGENTS.local.md` };
  const bin = resolveMysqlBin(root);
  const colSql = `SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME, ORDINAL_POSITION`;
  const idxSql = `SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX`;
  const cols = runQuery(root, cfg, bin, colSql);
  if (cols.error) return cols;
  const idxs = runQuery(root, cfg, bin, idxSql);
  if (idxs.error) return idxs;
  const tables = {};
  for (const [t, name, type, nullable, key, comment] of cols.rows) {
    if (!tables[t]) tables[t] = { columns: [], indexes: [] };
    tables[t].columns.push({ name, type: type.toLowerCase(), nullable: nullable === 'YES', key, comment });
  }
  const seen = new Set();
  for (const [t, name, nonUnique, col] of idxs.rows) {
    const k = `${t}.${name}`;
    if (!tables[t]) continue;
    if (!seen.has(k)) {
      seen.add(k);
      tables[t].indexes.push({ name, unique: nonUnique === '0', columns: [] });
    }
    tables[t].indexes.find((i) => i.name === name).columns.push(col);
  }
  return { schema: cfg.database, tables };
}

export function buildIndex(schema, tables, generatedAt) {
  const byPrefix = {};
  for (const t of Object.keys(tables).sort()) {
    const p = prefixOf(t);
    if (!byPrefix[p]) byPrefix[p] = {};
    byPrefix[p][t] = tables[t];
  }
  const files = {};
  for (const p of Object.keys(byPrefix).sort()) {
    files[p] = {
      _meta: {
        generated: true,
        source: 'information_schema',
        schema,
        generatedAt,
        notice: NOTICE,
        onCheckFail: ON_CHECK_FAIL,
        tablesCount: Object.keys(byPrefix[p]).length,
      },
      tables: byPrefix[p],
    };
  }
  return files;
}

// 结构化差异：返回 added/removed/changed 的表名（changed 附带列数变化摘要）
export function diffTables(oldT, newT) {
  const added = [];
  const removed = [];
  const changed = [];
  for (const t of Object.keys(newT)) {
    if (!oldT[t]) {
      added.push(t);
      continue;
    }
    const o = oldT[t];
    const n = newT[t];
    if (JSON.stringify(o) !== JSON.stringify(n)) {
      changed.push(`${t}（列 ${o.columns.length}→${n.columns.length}，索引 ${o.indexes.length}→${n.indexes.length}）`);
    }
  }
  for (const t of Object.keys(oldT)) if (!newT[t]) removed.push(t);
  return { added, removed, changed };
}

function loadCommitted(root) {
  const dir = join(root, OUT_DIR);
  const out = {};
  if (!existsSync(dir)) return out;
  for (const f of readdirSync(dir)) {
    const m = f.match(/^schema-index\.([a-zA-Z0-9]+)\.json$/);
    if (!m) continue;
    try {
      out[m[1]] = JSON.parse(readFileSync(join(dir, f), 'utf8'));
    } catch (e) {
      out[m[1]] = { _parseError: String(e.message) };
    }
  }
  return out;
}

export function main(argv = process.argv.slice(2)) {
  const mode = argv[0];
  const opt = { db: 'test', root: process.cwd() };
  for (let i = 1; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--db') opt.db = argv[++i];
    else if (a === '--root') opt.root = argv[++i];
    else {
      console.error(`未知参数：${a}`);
      process.exit(2);
    }
  }
  if (!['--generate', '--check'].includes(mode) || !['test', 'prod'].includes(opt.db)) {
    console.error('用法：node scripts/agent/schema-index.mjs --generate|--check [--db test|prod] [--root DIR]');
    process.exit(2);
  }
  const fetched = fetchSchema(opt.root, opt.db);
  if (fetched.error) {
    console.error(fetched.error);
    console.error('已提交生成物未被修改。');
    process.exit(3);
  }
  const files = buildIndex(fetched.schema, fetched.tables, new Date().toISOString());
  const dir = join(opt.root, OUT_DIR);

  if (mode === '--generate') {
    if (!existsSync(dir)) {
      console.error(`输出目录不存在：${dir}`);
      process.exit(3);
    }
    const stale = Object.keys(loadCommitted(opt.root)).filter((p) => !files[p]);
    for (const p of stale) unlinkSync(join(dir, `schema-index.${p}.json`));
    for (const [p, obj] of Object.entries(files)) {
      const text = JSON.stringify(obj, null, 2) + '\n';
      writeFileSync(join(dir, `schema-index.${p}.json`), text, 'utf8');
      const mb = (Buffer.byteLength(text) / 1024 / 1024).toFixed(2);
      console.log(`schema-index.${p}.json  ${obj._meta.tablesCount} 张表  ${mb} MB${Number(mb) > 2 ? '  ⚠️ 超过 2MB，建议进一步细分前缀' : ''}`);
    }
    if (stale.length) console.log(`清理过期前缀文件：${stale.join(', ')}`);
    return;
  }

  // --check：内存重生成 vs 已提交文件（忽略 generatedAt）
  const committed = loadCommitted(opt.root);
  const problems = [];
  for (const [p, obj] of Object.entries(files)) {
    const old = committed[p];
    if (!old) {
      problems.push(`前缀 ${p}：已提交文件缺失（新生成 ${obj._meta.tablesCount} 张表）`);
      continue;
    }
    if (old._parseError) {
      problems.push(`前缀 ${p}：已提交文件 JSON 损坏（${old._parseError}）`);
      continue;
    }
    const d = diffTables(old.tables || {}, obj.tables);
    if (d.added.length || d.removed.length || d.changed.length) {
      problems.push(`前缀 ${p}：新增表 ${d.added.length}、缺失表 ${d.removed.length}、结构变化 ${d.changed.length}${d.changed.length ? ' — ' + d.changed.slice(0, 3).join('；') : ''}`);
    }
  }
  for (const p of Object.keys(committed)) {
    if (!files[p]) problems.push(`前缀 ${p}：已提交文件在库中已无对应表（应删除该文件）`);
  }
  if (problems.length) {
    console.log(`schema-index 漂移（${opt.db} 库 vs 已提交生成物）：`);
    for (const s of problems) console.log(`  - ${s}`);
    console.log(`处置：${ON_CHECK_FAIL}`);
    process.exit(1);
  }
  console.log(`schema-index 一致（${Object.keys(committed).length} 个前缀文件，${Object.keys(fetched.tables).length} 张表）。`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
