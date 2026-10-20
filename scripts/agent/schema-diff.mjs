#!/usr/bin/env node
// schema-diff.mjs —— 两库结构差异报告（只读 information_schema，不查业务数据）
// 用法：node scripts/agent/schema-diff.mjs [--left test|prod] [--right test|prod] [--root DIR]
// 输出：docs/agent/schema-diff.md（生成物，含两侧库名与生成时间）
// 退出码：0 报告生成（有无差异都报告）；1 任一侧不可达（不覆盖已有报告，给出配置提示）。

import { writeFileSync, readFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { fetchSchema } from './schema-index.mjs';

const OUT = join('docs', 'agent', 'schema-diff.md');

// 纯差异计算：表 / 列（类型、可空）/ 索引，全部按 table.column 精确定位
export function diffSchema(left, right) {
  const tablesOnlyInLeft = Object.keys(left).filter((t) => !right[t]).sort();
  const tablesOnlyInRight = Object.keys(right).filter((t) => !left[t]).sort();
  const columns = { onlyInLeft: [], onlyInRight: [], typeChanged: [], nullChanged: [] };
  const indexes = { onlyInLeft: [], onlyInRight: [], columnsChanged: [] };
  for (const t of Object.keys(left).sort()) {
    if (!right[t]) continue;
    const lc = new Map(left[t].columns.map((c) => [c.name, c]));
    const rc = new Map(right[t].columns.map((c) => [c.name, c]));
    for (const [name, c] of lc) {
      if (!rc.has(name)) columns.onlyInLeft.push(`${t}.${name}`);
      else {
        const o = rc.get(name);
        if (o.type !== c.type) columns.typeChanged.push(`${t}.${name}（${c.type} → ${o.type}）`);
        if (o.nullable !== c.nullable) columns.nullChanged.push(`${t}.${name}（${c.nullable ? 'NULL' : 'NOT NULL'} → ${o.nullable ? 'NULL' : 'NOT NULL'}）`);
      }
    }
    for (const name of rc.keys()) if (!lc.has(name)) columns.onlyInRight.push(`${t}.${name}`);
    const li = new Map(left[t].indexes.map((i) => [i.name, i]));
    const ri = new Map(right[t].indexes.map((i) => [i.name, i]));
    for (const [name, i] of li) {
      if (!ri.has(name)) indexes.onlyInLeft.push(`${t}.${name}`);
      else if (JSON.stringify(ri.get(name).columns) !== JSON.stringify(i.columns)) indexes.columnsChanged.push(`${t}.${name}（${i.columns.join(',')} → ${ri.get(name).columns.join(',')}）`);
    }
    for (const name of ri.keys()) if (!li.has(name)) indexes.onlyInRight.push(`${t}.${name}`);
  }
  return { tablesOnlyInLeft, tablesOnlyInRight, columns, indexes };
}

function section(title, rows) {
  if (!rows.length) return `### ${title}\n\n无\n`;
  return `### ${title}\n\n| 位置 | 说明 |\n|---|---|\n${rows.map((r) => `| \`${r.split('（')[0]}\` | ${r.includes('（') ? r.slice(r.indexOf('（') + 1, -1) : '-'} |`).join('\n')}\n`;
}

export function renderReport(leftSchema, rightSchema, diff, stamp) {
  const anyDiff =
    diff.tablesOnlyInLeft.length || diff.tablesOnlyInRight.length ||
    Object.values(diff.columns).some((a) => a.length) ||
    Object.values(diff.indexes).some((a) => a.length);
  const parts = [
    `# 双库结构差异报告（生成物，勿手改）`,
    '',
    `> 生成时间：${stamp}`,
    `> 生成命令：\`node scripts/agent/schema-diff.mjs --left ${leftSchema.profile} --right ${rightSchema.profile}\``,
    `> 左侧库：\`${leftSchema.schema}\`；右侧库：\`${rightSchema.schema}\`。只读 information_schema，不含任何业务数据。`,
    `> 口径：结构漂移只能来自"两侧迁移应用进度不同"；收敛动作见 AGENTS.md 数据库铁律 2（分叉收敛）。`,
    '',
    anyDiff ? `**结论：存在 ${diff.tablesOnlyInLeft.length + diff.tablesOnlyInRight.length} 张表级差异、${diff.columns.typeChanged.length + diff.columns.nullChanged.length + diff.columns.onlyInLeft.length + diff.columns.onlyInRight.length} 处列差异、${diff.indexes.onlyInLeft.length + diff.indexes.onlyInRight.length + diff.indexes.columnsChanged.length} 处索引差异。**` : '**结论：两库结构一致，无差异。**',
    '',
    section('仅左侧存在的表', diff.tablesOnlyInLeft),
    section('仅右侧存在的表', diff.tablesOnlyInRight),
    section('仅左侧存在的列', diff.columns.onlyInLeft),
    section('仅右侧存在的列', diff.columns.onlyInRight),
    section('列类型变化', diff.columns.typeChanged),
    section('列可空性变化', diff.columns.nullChanged),
    section('仅左侧存在的索引', diff.indexes.onlyInLeft),
    section('仅右侧存在的索引', diff.indexes.onlyInRight),
    section('索引列构成变化', diff.indexes.columnsChanged),
  ];
  return parts.join('\n') + '\n';
}

export function main(argv = process.argv.slice(2)) {
  const opt = { left: 'test', right: 'prod' };
  let root = null;
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--left') opt.left = argv[++i];
    else if (a === '--right') opt.right = argv[++i];
    else if (a === '--root') root = argv[++i];
    else {
      console.error(`未知参数：${a}`);
      process.exit(1);
    }
  }
  root = root || (process.argv[1] ? join(dirname(fileURLToPath(import.meta.url)), '..', '..') : process.cwd());
  if (!['test', 'prod'].includes(opt.left) || !['test', 'prod'].includes(opt.right)) {
    console.error('--left/--right 只支持 test|prod');
    process.exit(1);
  }
  const fetched = {};
  for (const side of ['left', 'right']) {
    const r = fetchSchema(root, opt[side]);
    if (r.error) {
      console.error(`[${side}=${opt[side]}] 不可达：${r.error}`);
      console.error('已有报告未被覆盖。请检查 MYSQL_HOST/MYSQL_USER/MYSQL_PWD 或 AGENTS.local.md 连接信息。');
      process.exit(1);
    }
    fetched[side] = { ...r, profile: opt[side] };
  }
  const diff = diffSchema(fetched.left.tables, fetched.right.tables);
  const now = new Date();
  const stamp = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  const report = renderReport(fetched.left, fetched.right, diff, stamp);
  const outPath = join(root, OUT);
  writeFileSync(outPath, report, 'utf8');
  console.log(`written: ${outPath}`);
  console.log(`tables: ${Object.keys(fetched.left.tables).length}(左) vs ${Object.keys(fetched.right.tables).length}(右)；表级差异 ${diff.tablesOnlyInLeft.length + diff.tablesOnlyInRight.length}，列差异 ${diff.columns.onlyInLeft.length + diff.columns.onlyInRight.length + diff.columns.typeChanged.length + diff.columns.nullChanged.length}，索引差异 ${diff.indexes.onlyInLeft.length + diff.indexes.onlyInRight.length + diff.indexes.columnsChanged.length}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
