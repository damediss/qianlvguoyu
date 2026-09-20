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

// 2. Flyway 迁移
const migDir = join(root, 'services', 'platform-api', 'src', 'main', 'resources', 'db', 'migration');
const migrations = readdirSync(migDir).filter((f) => f.endsWith('.sql'));
const versions = migrations
  .map((f) => /^V(.+?)__/.exec(f)?.[1])
  .filter(Boolean)
  .sort((a, b) => {
    const pa = a.split(/[._]/).map(Number);
    const pb = b.split(/[._]/).map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
      const d = (pa[i] ?? 0) - (pb[i] ?? 0);
      if (d !== 0) return d;
    }
    return 0;
  });
const latestVersion = versions.at(-1) ?? '(无)';

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

- 迁移目录：\`services/platform-api/src/main/resources/db/migration/\`
- 迁移数量：**${migrations.length}** 个 .sql
- 最新版本号：**${latestVersion}**
- 新增迁移取号规则：测试库（gjjddb2026）与生产库（gjjddbpro）历史已分叉，必须同时查两库 \`SELECT MAX(version) FROM flyway_schema_history\`，取最大值 + 间隔，不要靠文件名猜。

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
