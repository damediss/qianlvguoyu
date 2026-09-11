package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Retires employee number from the competition roster contract.
 *
 * <p>The column is kept nullable for historical rows and external reporting
 * compatibility. New competition flows no longer read or write it, and the
 * former employee-number uniqueness constraint is removed.
 */
public class V20260911_013__retire_competition_employee_no extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      makeEmployeeNoNullable(connection, statement);
      dropIndexIfExists(connection, statement, "competition_roster_item",
          "uk_competition_roster_item_employee_no");
    }
  }

  private void makeEmployeeNoNullable(Connection connection, Statement statement)
      throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(), null, "competition_roster_item", "employee_no")) {
      if (!columns.next() || "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"))) {
        return;
      }
    }
    statement.execute(
        "ALTER TABLE competition_roster_item MODIFY COLUMN employee_no VARCHAR(64) NULL");
  }

  private void dropIndexIfExists(
      Connection connection, Statement statement, String table, String indexName)
      throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(), null, table, false, false)) {
      while (indexes.next()) {
        String actualName = indexes.getString("INDEX_NAME");
        if (indexName.equalsIgnoreCase(actualName)) {
          statement.execute("ALTER TABLE " + table + " DROP INDEX " + indexName);
          return;
        }
      }
    }
  }
}
