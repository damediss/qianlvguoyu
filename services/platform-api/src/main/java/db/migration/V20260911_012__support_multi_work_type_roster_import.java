package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Stores the resolved organization/work-type/level on each roster row.
 *
 * <p>The import batch may cover more than one organization and work type, so
 * the old batch-level selectors become nullable.  Existing registration and
 * contestant-profile uniqueness is already item/work-type scoped by the
 * preceding 20260908.006 migration.
 */
public class V20260911_012__support_multi_work_type_roster_import extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      makeBatchOrganizationNullable(connection, statement);
      addColumnIfMissing(connection, statement, "competition_roster_item", "org_id",
          "BIGINT NULL");
      addColumnIfMissing(connection, statement, "competition_roster_item", "org_code",
          "VARCHAR(128) NULL");
      addColumnIfMissing(connection, statement, "competition_roster_item", "org_name",
          "VARCHAR(255) NULL");
      addColumnIfMissing(connection, statement, "competition_roster_item", "event_work_type_id",
          "BIGINT NULL");
      addColumnIfMissing(connection, statement, "competition_roster_item", "event_work_type_level_id",
          "BIGINT NULL");
      addColumnIfMissing(connection, statement, "competition_roster_item", "source_row_no",
          "INT NULL");

      // The former key rejected the same employee appearing for another work type.
      dropIndexIfExists(connection, statement, "competition_roster_item",
          "uk_competition_roster_item_employee_no");
      addIndexIfMissing(connection, statement, "competition_roster_item",
          "idx_competition_roster_item_scope",
          "KEY idx_competition_roster_item_scope "
              + "(tenant_id, batch_id, org_id, event_work_type_id, "
              + "event_work_type_level_id, deleted)");
      addIndexIfMissing(connection, statement, "competition_roster_import",
          "idx_competition_roster_import_event_work_type",
          "KEY idx_competition_roster_import_event_work_type "
              + "(tenant_id, event_id, event_work_type_id, deleted)");
    }
  }

  private void makeBatchOrganizationNullable(Connection connection, Statement statement)
      throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(), null, "competition_roster_import", "org_id")) {
      if (!columns.next() || "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"))) {
        return;
      }
    }
    statement.execute("ALTER TABLE competition_roster_import MODIFY COLUMN org_id BIGINT NULL");
  }

  private void addColumnIfMissing(
      Connection connection, Statement statement, String table, String column, String definition)
      throws SQLException {
    if (columnExists(connection, table, column)) {
      return;
    }
    statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
  }

  private void addIndexIfMissing(
      Connection connection, Statement statement, String table, String indexName, String clause)
      throws SQLException {
    if (indexExists(connection, table, indexName)) {
      return;
    }
    statement.execute("ALTER TABLE " + table + " ADD " + clause);
  }

  private void dropIndexIfExists(
      Connection connection, Statement statement, String table, String indexName)
      throws SQLException {
    if (!indexExists(connection, table, indexName)) {
      return;
    }
    statement.execute("ALTER TABLE " + table + " DROP INDEX " + indexName);
  }

  private boolean columnExists(Connection connection, String table, String column) throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(), null, table, column)) {
      return columns.next();
    }
  }

  private boolean indexExists(Connection connection, String table, String indexName) throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(), null, table, false, false)) {
      while (indexes.next()) {
        String name = indexes.getString("INDEX_NAME");
        if (indexName.equalsIgnoreCase(name)) {
          return true;
        }
      }
    }
    return false;
  }
}
