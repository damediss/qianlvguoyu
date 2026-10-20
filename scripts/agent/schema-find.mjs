#!/usr/bin/env node
// schema-find.mjs —— 一条命令完成"字段在哪"五步取证（见 docs/agent/数据关系速查.md）
// 用法：node scripts/agent/schema-find.mjs <keyword> [--json] [--db test|prod|none] [--limit N] [--root DIR]
// 退出码：0 正常（无命中也是 0）；2 参数错误；3 依赖缺失（--db 非 none 但无 mysql 客户端/无连接信息）
// 约定：只输出 file:line / 表.列 供人判断，不做"自动结论"；--db prod 仅只读 information_schema。

import { readdirSync, statSync, readFileSync, existsSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const DB_DIRS = ['migration', 'migration-test', 'migration-prod']; // reference/ 是 legacy 快照，不检索

function* walk(dir, rootLen) {
  let names;
  try {
    names = readdirSync(dir);
  } catch {
    return;
  }
  for (const name of names) {
    const p = join(dir, name);
    let st;
    try {
      st = statSync(p);
    } catch {
      continue;
    }
    if (st.isDirectory()) yield* walk(p, rootLen);
    else yield p;
  }
}

function toCamel(s) {
  return s.replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase());
}
function toSnake(s) {
  return s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
}

// 候选键：关键词自身 + 驼峰/下划线互转；train_/exam_ 前缀表名额外取尾词（反向引用规则，方案 T2 检索面第 4 条）：
// 尾词 = 剥前缀后去集合后缀（_list/_detail/_info/_log），再生成 base 与 base+Id 的蛇形/驼峰四键
const COLLECTION_SUFFIXES = ['_list', '_detail', '_info', '_log'];

export function collectCandidates(keyword) {
  const keys = new Set([keyword, toCamel(keyword), toSnake(keyword)]);
  const m = keyword.match(/^(?:train|exam)_([a-z0-9_]+)$/);
  if (m) {
    const bases = [m[1]];
    for (const suf of COLLECTION_SUFFIXES) {
      if (m[1].endsWith(suf) && m[1].length > suf.length) bases.push(m[1].slice(0, -suf.length));
    }
    for (const base of bases) {
      keys.add(base);
      keys.add(toCamel(base));
      keys.add(`${base}_id`);
      keys.add(`${toCamel(base)}Id`);
    }
  }
  keys.delete('');
  return [...keys];
}

function classify(file, root) {
  const rel = relative(root, file).split(sep).join('/');
  if (rel.includes('src/main/resources/db/') && DB_DIRS.some((d) => rel.includes(`db/${d}/`)) && rel.endsWith('.sql'))
    return 'migrations';
  if (rel.includes('/entity/') && rel.endsWith('.java')) return 'entities';
  if (rel.endsWith('.java') || rel.endsWith('.xml')) return 'references';
  return null;
}

// 检索面：迁移 SQL / 实体 / Mapper 与 Service（纯文本命中，不解析 SQL 结构）
export function searchRepo(root, keyword) {
  const keys = collectCandidates(keyword).map((k) => k.toLowerCase());
  const hits = { migrations: [], entities: [], references: [] };
  const dirs = [
    join(root, 'services/platform-api/src/main/resources/db'),
    join(root, 'services/platform-api/src/main/java'),
  ];
  const files = [];
  for (const d of dirs) for (const f of walk(d)) files.push(f);
  files.sort();
  for (const file of files) {
    const section = classify(file, root);
    if (!section) continue;
    let lines;
    try {
      lines = readFileSync(file, 'utf8').split('\n');
    } catch {
      continue;
    }
    const rel = relative(root, file).split(sep).join('/');
    for (let i = 0; i < lines.length; i++) {
      const low = lines[i].toLowerCase();
      if (!keys.some((k) => low.includes(k))) continue;
      const text = lines[i].trim().slice(0, 140);
      hits[section].push({ file: rel, line: i + 1, text });
    }
  }
  return hits;
}

// 连接信息解析：AGENTS.local.md 打底，环境变量逐字段覆盖（方案 §6）；只读用途
export function resolveDbConfig(root, profile) {
  const env = process.env;
  let base = null;
  const local = join(root, 'AGENTS.local.md');
  if (existsSync(local)) {
    try {
      const md = readFileSync(local, 'utf8');
      const url = md.match(/jdbc:mysql:\/\/([^:/]+):(\d+)\/(\w+)/);
      const user = md.match(/MYSQL_USERNAME`\s*\|\s*`([^`]+)`/) || md.match(/MYSQL_USER`\s*\|\s*`([^`]+)`/);
      const pwd = md.match(/MYSQL_PASSWORD`\s*\|\s*`([^`]+)`/);
      if (url && user) base = { host: url[1], port: url[2], database: url[3], user: user[1], password: pwd ? pwd[1] : undefined };
    } catch {
      base = null;
    }
  }
  const cfg = {
    host: env.MYSQL_HOST || base?.host,
    port: env.MYSQL_PORT || base?.port || '3306',
    user: env.MYSQL_USER || env.MYSQL_USERNAME || base?.user,
    password: env.MYSQL_PASSWORD || env.MYSQL_PWD || base?.password,
  };
  const defaultDb = profile === 'prod' ? 'gjjddbpro' : 'gjjddb2026';
  cfg.database = profile === 'prod' ? (env.MYSQL_DATABASE_PROD || defaultDb) : (env.MYSQL_DATABASE || base?.database || defaultDb);
  if (!cfg.host || !cfg.user) return null;
  return cfg;
}

export function resolveMysqlBin(root) {
  if (process.env.MYSQL_BIN) return process.env.MYSQL_BIN;
  const local = join(root, 'AGENTS.local.md');
  if (existsSync(local)) {
    const md = readFileSync(local, 'utf8');
    const m = md.match(/mysql:\s*`([^`]+mysql\.exe)`/) || md.match(/MYSQL_BIN.*?\|\s*`([^`]+)`/);
    if (m) return m[1];
  }
  return 'mysql';
}

function escapeLike(s) {
  return s.replace(/[\\%_']/g, (c) => '\\' + c);
}

export function queryDb(root, profile, keyword) {
  const cfg = resolveDbConfig(root, profile);
  if (!cfg || !cfg.password) {
    return { error: `缺少连接信息：请 export MYSQL_HOST/MYSQL_USER/MYSQL_PWD（${profile}），或在仓库根提供 AGENTS.local.md` };
  }
  const bin = resolveMysqlBin(root);
  const kw = escapeLike(keyword);
  const sql = [
    `SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT FROM information_schema.COLUMNS`,
    `WHERE TABLE_SCHEMA = DATABASE() AND (TABLE_NAME LIKE '%${kw}%' OR COLUMN_NAME LIKE '%${kw}%' OR COLUMN_COMMENT LIKE '%${kw}%')`,
    `ORDER BY TABLE_NAME, ORDINAL_POSITION LIMIT 200`,
    `;`,
    `SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS`,
    `WHERE TABLE_SCHEMA = DATABASE() AND (INDEX_NAME LIKE '%${kw}%' OR COLUMN_NAME LIKE '%${kw}%')`,
    `GROUP BY TABLE_NAME, INDEX_NAME LIMIT 100`,
  ].join(' ');
  const r = spawnSync(bin, [
    '-h', cfg.host, '-P', String(cfg.port), '-u', cfg.user,
    `--database=${cfg.database}`, '--default-character-set=utf8mb4',
    '--batch', '--skip-column-names', '--connect-timeout=5', '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: cfg.password }, encoding: 'utf8', timeout: 10000, maxBuffer: 8 * 1024 * 1024 });
  if (r.error) {
    const missing = r.error.code === 'ENOENT';
    return { error: missing ? `找不到 mysql 客户端（${bin}）：请安装、加入 PATH 或 export MYSQL_BIN` : `mysql 调用失败：${r.error.message}` };
  }
  if (r.status !== 0) return { error: `mysql 错误（exit ${r.status}）：${(r.stderr || '').split('\n')[0]}` };
  const db = [];
  for (const line of (r.stdout || '').split('\n')) {
    if (!line.trim()) continue;
    const c = line.split('\t');
    if (c.length === 5) db.push(`${c[0]}.${c[1]} ${c[2]} ${c[3] === 'YES' ? 'NULL' : 'NOT NULL'}${c[4] ? ` COMMENT '${c[4]}'` : ''}`);
    else if (c.length === 3) db.push(`${c[0]} INDEX ${c[1]} (${c[2]})`);
  }
  return { db };
}

export function formatText(keyword, hits, dbHits, limit) {
  const out = [];
  let remaining = 0;
  // 每段保底 3 行，防止宽关键词（如 level/work_type_id）让迁移段吃光预算、 bury 掉 db 权威段
  const sections = [
    ['migrations', '[migrations]'],
    ['entities', '[entities]'],
    ['references', '[references]'],
  ];
  const reservePer = 3;
  let budget = limit;
  const all = [...sections.map(([k, h]) => [k, h, hits[k]]), ...(dbHits && !dbHits.error ? [['db', '[db]', dbHits.db]] : [])];
  all.forEach(([key, header, list], idx) => {
    const reserve = reservePer * (all.length - 1 - idx);
    const cap = Math.max(0, Math.min(list.length, budget - reserve));
    const shown = list.slice(0, cap);
    budget -= shown.length;
    remaining += list.length - shown.length;
    out.push(`${header} ${list.length} hit${list.length === 1 ? '' : 's'}`);
    for (const h of shown) out.push(typeof h === 'string' ? `  ${h}` : `  ${h.file}:${h.line}  ${h.text}`);
  });
  if (dbHits && dbHits.error) out.push(`[db] ${dbHits.error}`);
  if (remaining > 0) out.push(`还有 ${remaining} 条，--limit 查看`);
  return out.join('\n');
}

export function main(argv = process.argv.slice(2)) {
  const keyword = argv[0];
  const opt = { db: 'none', json: false, limit: 40, root: process.cwd() };
  for (let i = 1; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--json') opt.json = true;
    else if (a === '--db') opt.db = argv[++i];
    else if (a === '--limit') opt.limit = parseInt(argv[++i], 10);
    else if (a === '--root') opt.root = argv[++i];
    else {
      console.error(`未知参数：${a}`);
      process.exit(2);
    }
  }
  if (!keyword || Number.isNaN(opt.limit) || opt.limit < 1) {
    console.error('用法：node scripts/agent/schema-find.mjs <keyword> [--json] [--db test|prod|none] [--limit N] [--root DIR]');
    process.exit(2);
  }
  if (!['none', 'test', 'prod'].includes(opt.db)) {
    console.error(`--db 只支持 test|prod|none，收到：${opt.db}`);
    process.exit(2);
  }
  const hits = searchRepo(opt.root, keyword);
  let dbHits = null;
  if (opt.db !== 'none') {
    dbHits = queryDb(opt.root, opt.db, keyword);
    if (dbHits.error && dbHits.error.includes('找不到 mysql') || dbHits.error && dbHits.error.includes('缺少连接信息')) {
      console.error(dbHits.error);
      process.exit(3);
    }
  }
  if (opt.json) {
    console.log(JSON.stringify({ keyword, migrations: hits.migrations, entities: hits.entities, references: hits.references, db: dbHits ? (dbHits.db || null) : undefined, dbError: dbHits && dbHits.error ? dbHits.error : undefined }, null, 2));
  } else {
    console.log(formatText(keyword, hits, dbHits, opt.limit));
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
