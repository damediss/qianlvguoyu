import test from 'node:test';
import assert from 'node:assert/strict';
import {
  flywayFileStem,
  splitSqlStatements,
  isIgnorableMigrationError,
  parseMigrationFilename,
  compareFlywayVersion,
} from '../lib/flyway-repair.mjs';

test('parseMigrationFilename and version order', () => {
  const parsed = parseMigrationFilename('V20260603_018__add_preparation_equipment_config.sql');
  assert.equal(parsed.version, '20260603.018');
  assert.ok(compareFlywayVersion('20260603.018', '20260605.010') < 0);
  assert.equal(flywayFileStem('20260603.011'), 'V20260603_011');
  assert.equal(flywayFileStem('20260611.00201'), 'V20260611_00201');
});

test('splitSqlStatements keeps remaining DROP COLUMN after DROP INDEX', () => {
  const statements = splitSqlStatements(`
ALTER TABLE certification_info_exam_staff
  DROP INDEX uk_cert_info_exam_staff_cert;

ALTER TABLE certification_info_exam_staff
  DROP COLUMN user_id,
  DROP COLUMN remark;
`);
  assert.equal(statements.length, 2);
  assert.match(statements[0], /DROP INDEX/);
  assert.match(statements[1], /DROP COLUMN user_id/);
});

test('splitSqlStatements honors DELIMITER and skips comments', () => {
  const statements = splitSqlStatements(`
-- comment
DELIMITER $$
SELECT 1$$
DELIMITER ;
SELECT 2;
`);
  assert.deepEqual(statements, ['SELECT 1', 'SELECT 2']);
});

test('schema-fork DDL errors are ignorable; SQL syntax is not', () => {
  assert.equal(isIgnorableMigrationError("Can't DROP 'uk_x'; check that column/key exists"), true);
  assert.equal(isIgnorableMigrationError("Duplicate column name 'update_time'"), true);
  assert.equal(
    isIgnorableMigrationError(
      "BLOB/TEXT column 'item_value' used in key specification without a key length",
      'ALTER TABLE sys_dict_item MODIFY item_value TEXT',
    ),
    true,
  );
  assert.equal(
    isIgnorableMigrationError("You have an error in your SQL syntax; check the manual"),
    false,
  );
});
