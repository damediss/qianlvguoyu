import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, execFileSync } from 'node:child_process';
import { scrapeHealth } from './lib/collector.mjs';
import { repairFailedFlyway, applyPendingFlyway } from './lib/flyway-repair.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..', '..');
const runDir = path.join(here, '.run');
const composeFile = path.join(here, 'docker-compose.yml');
const PROJECT = 'formal-exam-load';
const REPAIR_LOG = path.join(runDir, 'flyway-repair.json');

function loadDotEnv() {
  const envPath = path.join(here, '.env');
  const env = { ...process.env };
  if (!fs.existsSync(envPath)) return env;
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    if (!line || line.trim().startsWith('#') || !line.includes('=')) continue;
    const index = line.indexOf('=');
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim();
    if (!(key in env)) env[key] = value;
  }
  return env;
}

function compose(args, env) {
  execFileSync('docker', ['compose', '-p', PROJECT, '-f', composeFile, ...args], {
    cwd: here,
    stdio: 'inherit',
    env,
  });
}

function dockerPs(env) {
  try {
    return execFileSync('docker', ['compose', '-p', PROJECT, '-f', composeFile, 'ps', '--format', 'json'], {
      cwd: here,
      encoding: 'utf8',
      env,
    });
  } catch {
    return '';
  }
}

function findJava(env) {
  if (env.JAVA_HOME && fs.existsSync(path.join(env.JAVA_HOME, 'bin', 'java.exe'))) {
    return path.join(env.JAVA_HOME, 'bin', 'java.exe');
  }
  if (env.JAVA_HOME && fs.existsSync(path.join(env.JAVA_HOME, 'bin', 'java'))) {
    return path.join(env.JAVA_HOME, 'bin', 'java');
  }
  const candidates = [
    'C:\\Program Files\\Java',
    'D:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Microsoft',
  ];
  for (const root of candidates) {
    if (!fs.existsSync(root)) continue;
    const found = fs.readdirSync(root).sort().reverse().find((name) => name.includes('21') || name.includes('jdk'));
    if (found) {
      const exe = path.join(root, found, 'bin', 'java.exe');
      if (fs.existsSync(exe)) return exe;
    }
  }
  return 'java';
}

function jarPath() {
  return path.join(repoRoot, 'services', 'platform-api', 'target', 'platform-api-0.1.0-SNAPSHOT.jar');
}

function apiJvmArgs(nodeName, env) {
  const nodePrefix = nodeName === 'api-2' ? 'API_2' : 'API_1';
  const value = (suffix, fallback) => env[`${nodePrefix}_${suffix}`] || env[`FORMAL_EXAM_API_${suffix}`] || fallback;
  const errorFile = path.join(runDir, `hs_err_pid%p-${nodeName}.log`);
  return [
    `-Xms${value('XMS', '256m')}`,
    `-Xmx${value('XMX', '1024m')}`,
    `-Xss${value('XSS', '512k')}`,
    `-XX:MaxMetaspaceSize=${value('MAX_METASPACE', '384m')}`,
    `-XX:MaxDirectMemorySize=${value('MAX_DIRECT_MEMORY', '256m')}`,
    `-XX:ReservedCodeCacheSize=${value('CODE_CACHE', '128m')}`,
    '-XX:+ExitOnOutOfMemoryError',
    `-XX:ErrorFile=${errorFile}`,
  ];
}

function appendBanner(nodeName, text) {
  fs.mkdirSync(runDir, { recursive: true });
  fs.appendFileSync(path.join(runDir, `${nodeName}.out.log`), `\n===== ${text} =====\n`);
}

function startApi(nodeName, port, env) {
  fs.mkdirSync(runDir, { recursive: true });
  const java = findJava(env);
  const jvmArgs = apiJvmArgs(nodeName, env);
  appendBanner(nodeName, `START ${new Date().toISOString()} port=${port} jvm=${jvmArgs.join(' ')}`);
  const out = fs.openSync(path.join(runDir, `${nodeName}.out.log`), 'a');
  const err = fs.openSync(path.join(runDir, `${nodeName}.err.log`), 'a');
  const child = spawn(java, [
    ...jvmArgs,
    `-Dspring.profiles.active=loadtest`,
    `-Dserver.port=${port}`,
    '-jar',
    jarPath(),
  ], {
    cwd: repoRoot,
    env: {
      ...env,
      SPRING_PROFILES_ACTIVE: 'loadtest',
      SERVER_PORT: String(port),
      COMPETITION_FORMAL_EXAM_NODE_ID: nodeName,
      REDIS_HOST: env.REDIS_HOST || '127.0.0.1',
      REDIS_PORT: env.REDIS_PORT || env.LOAD_REDIS_PORT || '16379',
      RABBITMQ_HOST: env.RABBITMQ_HOST || '127.0.0.1',
      RABBITMQ_PORT: env.RABBITMQ_PORT || env.LOAD_RABBIT_PORT || '15671',
      FILE_STORAGE_PROVIDER: 'local',
      FILE_LOCAL_ROOT: path.join(runDir, 'files'),
      WEKNORA_ENABLED: 'false',
      RENCAI_ENABLED: 'false',
    },
    detached: true,
    stdio: ['ignore', out, err],
  });
  child.unref();
  fs.writeFileSync(path.join(runDir, `${nodeName}.pid`), String(child.pid));
  return child.pid;
}

function pidAlive(pid) {
  if (!pid) return false;
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

function stopPid(name) {
  const pidFile = path.join(runDir, `${name}.pid`);
  if (!fs.existsSync(pidFile)) return;
  const pid = Number(fs.readFileSync(pidFile, 'utf8').trim());
  if (pid) {
    try {
      execFileSync('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore' });
    } catch {
      try { process.kill(pid); } catch { /* already gone */ }
    }
  }
  fs.rmSync(pidFile, { force: true });
}

async function waitHealthyOrDead(port, pid, timeoutMs = 600000) {
  const deadline = Date.now() + timeoutMs;
  const url = `http://127.0.0.1:${port}/api`;
  while (Date.now() < deadline) {
    const health = await scrapeHealth(url);
    if (health.ok) return 'healthy';
    if (!pidAlive(pid)) return 'dead';
    await new Promise((resolve) => setTimeout(resolve, 2000));
  }
  return pidAlive(pid) ? 'timeout' : 'dead';
}

function appendRepairLog(entry) {
  let current = [];
  if (fs.existsSync(REPAIR_LOG)) {
    try { current = JSON.parse(fs.readFileSync(REPAIR_LOG, 'utf8')); } catch { current = []; }
  }
  if (!Array.isArray(current)) current = [];
  current.push({ at: new Date().toISOString(), ...entry });
  fs.writeFileSync(REPAIR_LOG, `${JSON.stringify(current, null, 2)}\n`);
}

async function bootApi1(env) {
  const port1 = Number(env.API_1_PORT || 18081);
  let pid1 = null;
  for (let attempt = 1; attempt <= 80; attempt += 1) {
    stopPid('api-1');
    pid1 = startApi('api-1', port1, env);
    console.log(`started api-1 pid=${pid1} attempt=${attempt}; waiting for Flyway/health`);
    const outcome = await waitHealthyOrDead(port1, pid1);
    if (outcome === 'healthy') {
      return { ok: true, pid: pid1, attempts: attempt };
    }
    console.error(`api-1 ${outcome} on attempt ${attempt}`);
    stopPid('api-1');
    try {
      const result = await repairFailedFlyway(env);
      appendRepairLog({ attempt, outcome, repaired: result.repaired });
      if (!result.repaired.length) {
        console.error('no failed flyway row to repair; not retrying blindly');
        return { ok: false, pid: pid1, attempts: attempt, outcome };
      }
      console.log(`repaired flyway versions: ${result.repaired.map((row) => row.version).join(', ')}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      appendRepairLog({ attempt, outcome, error: message });
      console.error(`flyway repair failed: ${message}`);
      return { ok: false, pid: pid1, attempts: attempt, error: message };
    }
  }
  return { ok: false, pid: pid1, attempts: 80 };
}

export async function main(argv = process.argv.slice(2)) {
  const command = argv[0] || 'status';
  const env = loadDotEnv();
  fs.mkdirSync(runDir, { recursive: true });
  if (command === 'start') {
    if (!fs.existsSync(path.join(here, '.env'))) {
      fs.copyFileSync(path.join(here, '.env.example'), path.join(here, '.env'));
    }
    compose(['up', '-d', 'mysql', 'redis', 'rabbitmq', 'proxy'], env);
    const jar = jarPath();
    if (!fs.existsSync(jar)) {
      console.error(`JAR missing: ${jar}. Run mvn -f services/platform-api/pom.xml package -DskipTests`);
      process.exitCode = 2;
      return;
    }
    console.log('applying pending Flyway scripts on isolated gjjddb_load (empty-DB forks ignored)');
    try {
      const pending = await applyPendingFlyway(env);
      appendRepairLog({ command: 'apply-pending', applied: pending.applied.length, remainingFiles: pending.remainingFiles });
      console.log(`pending flyway applied=${pending.applied.length} catalog=${pending.remainingFiles}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      appendRepairLog({ command: 'apply-pending', error: message });
      console.error(`apply-pending flyway failed: ${message}`);
      process.exitCode = 3;
      return;
    }
    stopPid('api-2');
    const boot = await bootApi1(env);
    if (!boot.ok) {
      console.error('api-1 failed to become healthy; not starting api-2');
      process.exitCode = 3;
      return;
    }
    const pid2 = startApi('api-2', Number(env.API_2_PORT || 18082), env);
    const ok2 = (await waitHealthyOrDead(Number(env.API_2_PORT || 18082), pid2, 180000)) === 'healthy';
    console.log(JSON.stringify({
      api1: true,
      api2: ok2,
      api1Pid: boot.pid,
      api2Pid: pid2,
      flywayAttempts: boot.attempts,
      proxy: `http://127.0.0.1:${env.LOAD_PROXY_PORT || 18080}/api`,
    }));
    if (!ok2) process.exitCode = 3;
    return;
  }
  if (command === 'repair-flyway') {
    const failed = await repairFailedFlyway(env);
    const pending = await applyPendingFlyway(env);
    appendRepairLog({
      command: 'repair-flyway',
      repaired: failed.repaired,
      applied: pending.applied.length,
    });
    console.log(JSON.stringify({ repaired: failed.repaired, applied: pending.applied.length }, null, 2));
    return;
  }
  if (command === 'stop') {
    stopPid('api-1');
    stopPid('api-2');
    compose(['stop'], env);
    return;
  }
  if (command === 'status') {
    const ps = dockerPs(env);
    const pids = ['api-1', 'api-2'].map((name) => {
      const file = path.join(runDir, `${name}.pid`);
      const pid = fs.existsSync(file) ? Number(fs.readFileSync(file, 'utf8').trim()) : null;
      return { name, pid, alive: pidAlive(pid) };
    });
    console.log(JSON.stringify({ compose: ps.trim().slice(0, 2000), apis: pids }, null, 2));
    return;
  }
  if (command === 'fault-redis') {
    compose(['stop', 'redis'], env);
    console.log('redis stopped for 30s');
    await new Promise((resolve) => setTimeout(resolve, 30_000));
    compose(['start', 'redis'], env);
    return;
  }
  if (command === 'restart-api-1' || command === 'restart-api-2') {
    const name = command === 'restart-api-1' ? 'api-1' : 'api-2';
    const port = name === 'api-1' ? Number(env.API_1_PORT || 18081) : Number(env.API_2_PORT || 18082);
    stopPid(name);
    await new Promise((resolve) => setTimeout(resolve, 2000));
    const pid = startApi(name, port, env);
    const ok = (await waitHealthyOrDead(port, pid)) === 'healthy';
    console.log(JSON.stringify({ restarted: name, pid, healthy: ok }));
    return;
  }
  if (command === 'fault-rabbit') {
    compose(['stop', 'rabbitmq'], env);
    console.log('rabbitmq stopped (coexistence observation only)');
    return;
  }
  console.error(`unknown command ${command}`);
  process.exitCode = 1;
}

if (process.argv[1] && path.normalize(process.argv[1]).endsWith('ctl.mjs')) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exit(1);
  });
}
