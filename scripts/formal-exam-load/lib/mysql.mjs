import mysql from 'mysql2/promise';
import { assertSeedMysql, REQUIRED_SCHEMA } from './gate.mjs';

export function mysqlConfigFromEnv(env = process.env) {
  return {
    host: env.LOAD_MYSQL_HOST || '127.0.0.1',
    port: Number(env.LOAD_MYSQL_PORT || 13306),
    user: env.LOAD_MYSQL_USER || env.MYSQL_USERNAME || 'gjjd_load',
    password: env.LOAD_MYSQL_PASSWORD || env.MYSQL_PASSWORD || 'LoadTest_Db_Only',
    database: env.LOAD_MYSQL_DATABASE || 'gjjddb_load',
    charset: 'utf8mb4',
    multipleStatements: false,
  };
}

export async function openMysql(env = process.env) {
  const config = mysqlConfigFromEnv(env);
  assertSeedMysql(config);
  const connection = await mysql.createConnection({
    ...config,
    decimalNumbers: true,
  });
  const [rows] = await connection.query('SELECT DATABASE() AS db, @@hostname AS hostname');
  const actual = rows && rows[0] ? rows[0].db : '';
  if (String(actual).toLowerCase() !== REQUIRED_SCHEMA) {
    await connection.end();
    throw new Error(`connected schema is ${actual}, required ${REQUIRED_SCHEMA}`);
  }
  return connection;
}

export async function tableColumns(connection, table) {
  const [rows] = await connection.query(
    `SELECT COLUMN_NAME AS name, IS_NULLABLE AS nullable, COLUMN_DEFAULT AS def,
            EXTRA AS extra, DATA_TYPE AS dataType
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?`,
    [table],
  );
  return rows || [];
}

export async function hasTable(connection, table) {
  const [rows] = await connection.query(
    `SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? LIMIT 1`,
    [table],
  );
  return Boolean(rows && rows[0]);
}

export async function insertRow(connection, table, row) {
  const columns = await tableColumns(connection, table);
  if (!columns.length) {
    throw new Error(`table ${table} does not exist in ${REQUIRED_SCHEMA}`);
  }
  const payload = {};
  for (const column of columns) {
    const extra = String(column.extra || '');
    if (extra.toLowerCase().includes('generated')) continue;
    if (Object.prototype.hasOwnProperty.call(row, column.name)) {
      payload[column.name] = row[column.name];
      continue;
    }
    if (column.def != null || extra.toLowerCase().includes('auto_increment')) continue;
    if (String(column.nullable).toUpperCase() === 'YES') continue;
    throw new Error(`missing required ${table}.${column.name}`);
  }
  const names = Object.keys(payload);
  const placeholders = names.map(() => '?').join(', ');
  const sql = `INSERT INTO ${table} (${names.map((name) => `\`${name}\``).join(', ')}) VALUES (${placeholders})`;
  await connection.execute(sql, names.map((name) => payload[name]));
}

export async function upsertById(connection, table, row) {
  const columns = await tableColumns(connection, table);
  const idColumn = columns.find((column) => column.name === 'id');
  if (!idColumn) {
    await insertRow(connection, table, row);
    return;
  }
  const [existing] = await connection.execute(
    `SELECT id FROM ${table} WHERE id = ? LIMIT 1`,
    [row.id],
  );
  if (existing && existing[0]) {
    return;
  }
  await insertRow(connection, table, row);
}

export async function query(connection, sql, params = []) {
  const [rows] = await connection.execute(sql, params);
  return rows || [];
}
