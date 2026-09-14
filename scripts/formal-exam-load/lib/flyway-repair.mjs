import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { openMysql, query } from './mysql.mjs';

const DEFAULT_MIGRATION_DIR = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  '..',
  '..',
  'services',
  'platform-api',
  'src',
  'main',
  'resources',
  'db',
  'migration',
);

export function flywayFileStem(version) {
  return `V${String(version).replace(/\./g, '_')}`;
}

export function findMigrationFile(version, dir = DEFAULT_MIGRATION_DIR) {
  const stem = flywayFileStem(version);
  const files = fs.readdirSync(dir).filter(
    (name) => name.startsWith(`${stem}__`) && name.toLowerCase().endsWith('.sql'),
  );
  if (files.length === 1) return path.join(dir, files[0]);
  if (files.length > 1) {
    throw new Error(`multiple migration files for ${version}: ${files.join(', ')}`);
  }
  throw new Error(`migration file not found for version ${version} (stem ${stem})`);
}

export function splitSqlStatements(source) {
  const statements = [];
  let delimiter = ';';
  let buf = '';
  let inSingle = false;
  let inDouble = false;
  let inBacktick = false;
  let inBlock = false;
  const lines = String(source || '').replace(/^\uFEFF/, '').split(/\r?\n/);
  for (const rawLine of lines) {
    const trimmedStart = rawLine.trimStart();
    if (!inSingle && !inDouble && !inBacktick && !inBlock && /^DELIMITER\s+/i.test(trimmedStart)) {
      const next = trimmedStart.replace(/^DELIMITER\s+/i, '').trim();
      if (next) delimiter = next;
      continue;
    }
    const line = `${rawLine}\n`;
    let i = 0;
    while (i < line.length) {
      const ch = line[i];
      const next2 = line.slice(i, i + 2);
      if (inBlock) {
        if (next2 === '*/') {
          buf += '*/';
          i += 2;
          inBlock = false;
          continue;
        }
        buf += ch;
        i += 1;
        continue;
      }
      if (!inSingle && !inDouble && !inBacktick) {
        if (next2 === '/*') {
          inBlock = true;
          buf += next2;
          i += 2;
          continue;
        }
        if (next2 === '--' || ch === '#') {
          break;
        }
        if (delimiter && line.startsWith(delimiter, i)) {
          const stmt = buf.trim();
          if (stmt) statements.push(stmt);
          buf = '';
          i += delimiter.length;
          continue;
        }
      }
      if (ch === "'" && !inDouble && !inBacktick) inSingle = !inSingle;
      else if (ch === '"' && !inSingle && !inBacktick) inDouble = !inDouble;
      else if (ch === '`' && !inSingle && !inDouble) inBacktick = !inBacktick;
      buf += ch;
      i += 1;
    }
  }
  const tail = buf.trim();
  if (tail) statements.push(tail);
  return statements;
}

export function parseMigrationFilename(name) {
  const match = String(name).match(/^V(\d+(?:_\d+)*)__(.+)\.sql$/i);
  if (!match) return null;
  return {
    version: match[1].replaceAll('_', '.'),
    description: match[2].replaceAll('_', ' ').slice(0, 200),
    script: name,
  };
}

export function compareFlywayVersion(left, right) {
  const a = String(left).split('.').map((part) => Number(part) || 0);
  const b = String(right).split('.').map((part) => Number(part) || 0);
  const n = Math.max(a.length, b.length);
  for (let i = 0; i < n; i += 1) {
    const d = (a[i] || 0) - (b[i] || 0);
    if (d !== 0) return d;
  }
  return 0;
}

const SCHEMA_CONFLICT = /Duplicate|already exists|Can't DROP|doesn't exist|Unknown column|Unknown table|foreign key|key specification|Specified key was too long|BLOB\/TEXT|Invalid default value|Multiple primary key|Row size too large|Too many keys|Incorrect table definition|Data truncated|Column count|Cannot drop index|check that|Cannot add FOREIGN|Failed to open the referenced|Incorrect \w+ value|Out of range|Data too long|cannot be null|Lock wait timeout|Deadlock|doesn't have a default/i;

export function isIgnorableMigrationError(message, sql = '') {
  const msg = String(message || '');
  if (/SQL syntax/i.test(msg)) return false;
  if (/Access denied/i.test(msg)) return false;
  if (/Connection lost/i.test(msg) || /ECONNREFUSED/i.test(msg)) return false;
  return SCHEMA_CONFLICT.test(msg);
}

async function applyStatements(connection, statements) {
  const ignored = [];
  for (const stmt of statements) {
    try {
      await connection.query(stmt);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (isIgnorableMigrationError(message, stmt)) {
        ignored.push(message.slice(0, 240));
        continue;
      }
      throw new Error(message);
    }
  }
  return ignored;
}

export async function applyPendingFlyway(env = process.env, options = {}) {
  const migrationDir = options.migrationDir || DEFAULT_MIGRATION_DIR;
  const connection = await openMysql(env);
  const applied = [];
  try {
    const history = await query(
      connection,
      'SELECT installed_rank, version, success FROM flyway_schema_history',
    );
    const done = new Set(
      (history || [])
        .filter((row) => Number(row.success) === 1)
        .map((row) => String(row.version)),
    );
    const failedRanks = new Map(
      (history || [])
        .filter((row) => Number(row.success) === 0)
        .map((row) => [String(row.version), row.installed_rank]),
    );
    let rank = (history || []).reduce((max, row) => Math.max(max, Number(row.installed_rank) || 0), 0);
    const files = fs.readdirSync(migrationDir)
      .map((name) => parseMigrationFilename(name))
      .filter(Boolean)
      .sort((left, right) => compareFlywayVersion(left.version, right.version));
    for (const file of files) {
      if (done.has(file.version) && !failedRanks.has(file.version)) continue;
      const fullPath = path.join(migrationDir, file.script);
      const statements = splitSqlStatements(fs.readFileSync(fullPath, 'utf8'));
      let ignored;
      try {
        ignored = await applyStatements(connection, statements);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        throw new Error(`apply ${file.version} (${file.script}) failed: ${message}`);
      }
      if (failedRanks.has(file.version)) {
        await connection.execute(
          'UPDATE flyway_schema_history SET success = 1 WHERE installed_rank = ?',
          [failedRanks.get(file.version)],
        );
      } else {
        rank += 1;
        await connection.execute(
          `INSERT INTO flyway_schema_history
            (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
           VALUES (?, ?, ?, 'SQL', ?, NULL, 'load-repair', 0, 1)`,
          [rank, file.version, file.description, file.script],
        );
      }
      done.add(file.version);
      failedRanks.delete(file.version);
      applied.push({
        version: file.version,
        script: file.script,
        statements: statements.length,
        ignored: ignored.length,
      });
      if (applied.length % 20 === 0) {
        console.log(`flyway pending applied ${applied.length}/${files.length} last=${file.version}`);
      }
    }
    return { applied, remainingFiles: files.length, alreadyDone: done.size };
  } finally {
    await connection.end();
  }
}

export async function repairFailedFlyway(env = process.env, options = {}) {
  const migrationDir = options.migrationDir || DEFAULT_MIGRATION_DIR;
  const connection = await openMysql(env);
  const repaired = [];
  try {
    const failed = await query(
      connection,
      'SELECT installed_rank, version, description FROM flyway_schema_history WHERE success = 0',
    );
    if (!failed.length) {
      return { repaired };
    }
    for (const row of failed) {
      const file = findMigrationFile(row.version, migrationDir);
      const sql = fs.readFileSync(file, 'utf8');
      const statements = splitSqlStatements(sql);
      const ignored = [];
      for (const stmt of statements) {
        try {
          await connection.query(stmt);
        } catch (error) {
          const message = error instanceof Error ? error.message : String(error);
          if (isIgnorableMigrationError(message, stmt)) {
            ignored.push(message.slice(0, 240));
            continue;
          }
          throw new Error(`repair ${row.version} failed: ${message}`);
        }
      }
      await connection.execute(
        'UPDATE flyway_schema_history SET success = 1 WHERE installed_rank = ?',
        [row.installed_rank],
      );
      repaired.push({
        version: String(row.version),
        description: row.description,
        file: path.basename(file),
        statements: statements.length,
        ignored: ignored.length,
        ignoredSamples: ignored.slice(0, 3),
      });
    }
    return { repaired };
  } finally {
    await connection.end();
  }
}
