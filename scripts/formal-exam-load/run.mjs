#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs, applyScenario } from './lib/args.mjs';
import { phase6Catalog, planPadsAndConsoles } from './lib/catalog.mjs';
import { normalizeBaseUrl } from './lib/gate.mjs';
import { seedLoadData, writeFixture } from './lib/seed.mjs';
import { runLive, proveIsolation } from './lib/runner.mjs';
import { openMysql } from './lib/mysql.mjs';
import { reconcile } from './lib/reconcile.mjs';
import { mergeReports } from './lib/aggregate.mjs';
import { scrapeHealth, listMetricNames, pickMetricNames, scrapeMetrics } from './lib/collector.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));

function loadLocalEnv() {
  const envPath = path.join(here, '.env');
  if (!fs.existsSync(envPath)) return;
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    if (!line || line.trim().startsWith('#') || !line.includes('=')) continue;
    const index = line.indexOf('=');
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim();
    if (!process.env[key]) process.env[key] = value;
  }
}

async function main() {
  loadLocalEnv();
  const options = applyScenario(parseArgs(), process.argv);
  options.baseUrl = normalizeBaseUrl(options.baseUrl);
  options.isolatedInfra = process.env.FORMAL_EXAM_LOAD_COMPOSE === '1' || fs.existsSync(path.join(here, '.run'));
  if (options.command === 'catalog' && !options.live) {
    const report = {
      dryRun: true,
      generatedAt: new Date().toISOString(),
      policy: {
        answersOnHttp: true,
        websocketStatusControlAckOnly: true,
        doNotInvent3000Capacity: true,
      },
      catalog: phase6Catalog(),
      selected: planPadsAndConsoles(options.pads, options.consoles),
    };
    process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
    return;
  }
  if (options.command === 'seed') {
    const fixture = await seedLoadData(options);
    const file = await writeFixture(fixture, path.join(here, '.run', 'fixture.json'));
    process.stdout.write(`${JSON.stringify({
      ok: true,
      fixture: file,
      sessionId: fixture.sessionId,
      pads: fixture.pads.length,
      questions: fixture.questions,
      checks: fixture.checks,
    }, null, 2)}\n`);
    return;
  }
  if (options.command === 'gate') {
    const isolation = await proveIsolation(options.baseUrl, options);
    process.stdout.write(`${JSON.stringify(isolation, null, 2)}\n`);
    if (!isolation.gate.ok) process.exitCode = 2;
    return;
  }
  if (options.command === 'metrics') {
    const tenantId = process.env.LOAD_TENANT_ID || '10159';
    const listed = await listMetricNames(options.baseUrl, tenantId);
    const picked = pickMetricNames(listed.names || []);
    const collected = await scrapeMetrics(options.baseUrl, tenantId, picked.wanted.concat(picked.extra.slice(0, 15)));
    process.stdout.write(`${JSON.stringify({ catalog: picked, collected }, null, 2)}\n`);
    return;
  }
  if (options.command === 'health') {
    process.stdout.write(`${JSON.stringify(await scrapeHealth(options.baseUrl), null, 2)}\n`);
    return;
  }
  if (options.command === 'reconcile') {
    const fixturePath = options.fixture || path.join(here, '.run', 'fixture.json');
    const fixture = JSON.parse(fs.readFileSync(fixturePath, 'utf8'));
    const connection = await openMysql();
    try {
      const result = await reconcile(connection, fixture, []);
      process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
      if (!result.ok) process.exitCode = 2;
    } finally {
      await connection.end();
    }
    return;
  }
  if (options.command === 'aggregate') {
    const dir = options.outputDir;
    const files = fs.readdirSync(dir).filter((name) => name.endsWith('live.json') || name === 'live.json');
    const reports = files.map((name) => JSON.parse(fs.readFileSync(path.join(dir, name), 'utf8')));
    process.stdout.write(`${JSON.stringify(mergeReports(reports), null, 2)}\n`);
    return;
  }
  if (options.command === 'live' || options.live) {
    if (options.mode === 'health') {
      process.stdout.write(`${JSON.stringify(await scrapeHealth(options.baseUrl), null, 2)}\n`);
      return;
    }
    if (options.mode === 'metrics') {
      const listed = await listMetricNames(options.baseUrl, process.env.LOAD_TENANT_ID || '10159');
      process.stdout.write(`${JSON.stringify(listed.body || listed, null, 2)}\n`);
      return;
    }
    const report = await runLive(options);
    process.stdout.write(`${JSON.stringify({
      blocked: Boolean(report.blocked),
      measured: report.measured,
      outputDir: report.outputDir,
      ws: report.ws,
      runner: report.runner,
      reconcileOk: report.reconcile ? report.reconcile.ok : null,
      gate: report.gate || report.isolation,
    }, null, 2)}\n`);
    if (report.blocked || (report.reconcile && !report.reconcile.ok)) process.exitCode = 2;
    return;
  }
  process.stderr.write(`unknown command ${options.command}\n`);
  process.exitCode = 1;
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
  process.exit(1);
});
