#!/usr/bin/env node
// 生成 docs/agent/项目现状.md —— 凡是能被命令算出来的，不写进 AGENTS.md 手工清单。
// 用法：node scripts/gen-agents-context.mjs（从仓库根目录执行）

import { readdirSync, statSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';

const root = process.cwd();
const out = join(root, 'docs', 'agent', '项目现状.md');

function* walk(dir) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) yield* walk(p);
    else yield p;
  }
}

function countFiles(dir, ext) {
  if (!existsSync(dir)) return 0;
  let n = 0;
  for (const f of walk(dir)) if (f.endsWith(ext)) n++;
  return n;
}

function listDirs(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).filter((n) => {
    try {
      return statSync(join(dir, n)).isDirectory();
    } catch {
      return false;
    }
  }).sort();
}

// 1. 后端包清单
const pkgRoot = join(root, 'services', 'platform-api', 'src', 'main', 'java', 'com', 'guoyu', 'platform');
const packages = listDirs(pkgRoot).map((name) => ({
  name,
  files: countFiles(join(pkgRoot, name), '.java'),
}));
const totalJava = packages.reduce((s, p) => s + p.files, 0);

// 2. Flyway 迁移（三档目录：共用 / 仅测试库历史 / 仅生产库历史）
const dbRoot = join(root, 'services', 'platform-api', 'src', 'main', 'resources', 'db');
const MIG_DIRS = [
  { key: 'shared', dir: 'migration', scope: '全部 profile' },
  { key: 'test', dir: 'migration-test', scope: '默认 + application-loadtest.yml（测试库历史）' },
  { key: 'prod', dir: 'migration-prod', scope: 'application-prod.yml（生产库历史）' },
];

function cmpVersion(a, b) {
  const pa = String(a).split(/[._]/).map(Number);
  const pb = String(b).split(/[._]/).map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (d !== 0) return d;
  }
  return 0;
}

function readMigrations(dirName) {
  const dir = join(dbRoot, dirName);
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((f) => f.endsWith('.sql'))
    .map((f) => ({
      file: f,
      version: /^V(.+?)__/.exec(f)?.[1] ?? '',
      name: f.replace(/^V.+?__/, '').replace(/\.sql$/, ''),
    }))
    .sort((a, b) => cmpVersion(a.version, b.version));
}

const migSets = Object.fromEntries(MIG_DIRS.map((d) => [d.key, readMigrations(d.dir)]));
const migrations = migSets.shared;
const latestVersion = migrations.at(-1)?.version ?? '(无)';

// 分叉双写对：同一份 DDL 在 test / prod 各写一遍，版本号不同（收敛前的常态债务）
const testByName = new Map(migSets.test.map((m) => [m.name, m]));
const prodByName = new Map(migSets.prod.map((m) => [m.name, m]));
const forkPairs = [...testByName.keys()]
  .filter((name) => prodByName.has(name))
  .map((name) => ({ name, test: testByName.get(name).version, prod: prodByName.get(name).version }))
  .sort((a, b) => a.name.localeCompare(b.name));

// 版本号硬冲突：两个目录存在相同版本号但内容不同的脚本。
// 运行时 locations 只加载「共用 + 其中一个分叉」，一旦有人把它们合并进同一 locations，
// Flyway 会直接拒绝启动（more than one migration with version X）。
function versionClashes(aKey, bKey, label) {
  const a = new Map(migSets[aKey].map((m) => [m.version, m]));
  const b = new Map(migSets[bKey].map((m) => [m.version, m]));
  return [...a.keys()]
    .filter((v) => b.has(v) && b.get(v).name !== a.get(v).name)
    .map((v) => ({ version: v, pair: label, a: a.get(v).name, b: b.get(v).name }))
    .sort((x, y) => cmpVersion(x.version, y.version));
}
const clashes = [
  ...versionClashes('shared', 'test', '共用 × test'),
  ...versionClashes('shared', 'prod', '共用 × prod'),
  ...versionClashes('test', 'prod', 'test × prod'),
];

// 3. 前端页面目录
const pages = listDirs(join(root, 'apps', 'admin-web', 'src', 'pages'));

// 4. apps / services 状态
function implStatus(dir) {
  if (!existsSync(dir)) return '不存在';
  const hasSrc = existsSync(join(dir, 'src')) || existsSync(join(dir, 'app'));
  const hasBuild = ['pom.xml', 'package.json', 'build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts']
    .some((f) => existsSync(join(dir, f)));
  if (hasSrc && hasBuild) return '有实现';
  if (hasSrc) return '有源码目录，无构建描述';
  return '仅 README / 空壳';
}
const apps = listDirs(join(root, 'apps')).map((n) => ({ name: n, status: implStatus(join(root, 'apps', n)) }));
const services = listDirs(join(root, 'services')).map((n) => ({ name: n, status: implStatus(join(root, 'services', n)) }));

const now = new Date();
const stamp = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

const md = `# 项目现状（脚本生成，勿手改）

> 生成时间：${stamp}
> 生成命令：\`node scripts/gen-agents-context.mjs\`
> 用途：AGENTS.md 不承载快照类信息；模块清单、迁移基线、服务状态以本文件为准。

## 后端包清单（services/platform-api，共 ${packages.length} 个包 / ${totalJava} 个 Java 文件）

| 包 | Java 文件数 |
|---|---|
${packages.map((p) => `| \`${p.name}\` | ${p.files} |`).join('\n')}

## Flyway 迁移基线

> 目录三档的来龙去脉与操作铁律见 \`AGENTS.md\` 数据库铁律 2；本文件只承载**算得出来的事实**。

- 迁移目录三档（profile 接线在 \`application.yml\` / \`application-prod.yml\` 的 \`spring.flyway.locations\`）：

| 目录 | 脚本数 | 最新版本号 | 生效范围 |
|---|---|---|---|
${MIG_DIRS.map((d) => `| \`db/${d.dir}/\` | ${migSets[d.key].length} | ${migSets[d.key].at(-1)?.version ?? '(无)'} | ${d.scope} |`).join('\n')}

- 新增**共用**迁移取号：测试库（gjjddb2026）与生产库（gjjddbpro）历史已分叉，必须同时查两库 \`SELECT MAX(version) FROM flyway_schema_history\`，取最大值后再加间隔（间隔口径见 AGENTS.md），不要靠文件名猜。

### 分叉双写对（${forkPairs.length} 组）

同一份 DDL 在 test / prod 各写一遍、版本号还不同——分叉收敛前的历史债务，内容靠人工同步，无自动校验。

${forkPairs.length ? `| 脚本 | test 版本 | prod 版本 |\n|---|---|---|\n${forkPairs.map((p) => `| \`${p.name}\` | ${p.test} | ${p.prod} |`).join('\n')}` : '当前无分叉双写（历史已收敛为单线）。'}

### 版本号硬冲突（${clashes.length} 处）

两个目录里存在**相同版本号但内容不同**的脚本。运行时 locations 只应加载「共用 + 其中一个分叉」；一旦被合并进同一 locations，Flyway 会直接拒绝启动。

${clashes.length ? `| 版本号 | 冲突组合 | 一侧脚本 | 另一侧脚本 |\n|---|---|---|---|\n${clashes.map((c) => `| ${c.version} | ${c.pair} | \`${c.a}\` | \`${c.b}\` |`).join('\n')}` : '当前无同版本号冲突。'}

## 前端页面目录（apps/admin-web/src/pages，共 ${pages.length} 个）

${pages.map((p) => `\`${p}\``).join(' / ')}

## apps 状态

| 目录 | 状态 |
|---|---|
${apps.map((a) => `| \`${a.name}\` | ${a.status} |`).join('\n')}

## services 状态

| 目录 | 状态 |
|---|---|
${services.map((s) => `| \`${s.name}\` | ${s.status} |`).join('\n')}
`;

mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, md, 'utf8');
console.log(`written: ${out}`);
console.log(`packages=${packages.length} java=${totalJava} migrations=${migrations.length} latest=${latestVersion} pages=${pages.length}`);
console.log(`mig dirs: ${MIG_DIRS.map((d) => `${d.dir}=${migSets[d.key].length}`).join(' ')}`);
console.log(`forkPairs=${forkPairs.length} versionClashes=${clashes.length}`);
if (clashes.length) {
  console.warn(`[warn] 存在 ${clashes.length} 处同版本号冲突，locations 合并会启动失败`);
  // 冲突必须当次修掉（AGENTS.md 数据库铁律 2）：非零退出让调用方能卡住
  process.exitCode = 1;
}
