package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Allows one contestant to enroll in multiple event work types under the same event.
 * Event-level membership stays unique; item-level registration/profile uniqueness is per work type.
 */
public class V20260908_006__allow_multi_event_work_type_participation extends BaseJavaMigration {

  private static final String GENERATED =
      "BIGINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN IFNULL(event_work_type_id, 0) ELSE NULL END) STORED";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(connection, statement, "competition_registration", "event_work_type_id",
          "BIGINT NULL AFTER member_id");
      backfillRegistrationWorkType(connection);
      dropIndexIfExists(connection, statement, "competition_registration",
          "uk_competition_registration_event_member");
      addColumnIfMissing(connection, statement, "competition_registration", "item_work_type_key",
          GENERATED);
      addIndexIfMissing(connection, statement, "competition_registration",
          "uk_competition_registration_event_item",
          "UNIQUE KEY uk_competition_registration_event_item (tenant_id, event_id, member_id, item_work_type_key)");
      addIndexIfMissing(connection, statement, "competition_registration",
          "idx_competition_registration_event_work_type",
          "KEY idx_competition_registration_event_work_type (tenant_id, event_id, event_work_type_id, status, deleted)");

      dropIndexIfExists(connection, statement, "competition_contestant_profile",
          "uk_competition_contestant_profile_member");
      addColumnIfMissing(connection, statement, "competition_contestant_profile", "item_work_type_key",
          GENERATED);
      addIndexIfMissing(connection, statement, "competition_contestant_profile",
          "uk_competition_contestant_profile_member_item",
          "UNIQUE KEY uk_competition_contestant_profile_member_item (tenant_id, member_id, item_work_type_key)");
    }
  }

  private void backfillRegistrationWorkType(Connection connection) throws SQLException {
    try (PreparedStatement fromProfile = connection.prepareStatement(
        "UPDATE competition_registration r "
            + "JOIN competition_contestant_profile p "
            + "  ON p.registration_id = r.id AND p.tenant_id = r.tenant_id AND p.deleted = 0 "
            + "SET r.event_work_type_id = p.event_work_type_id "
            + "WHERE r.deleted = 0 AND r.event_work_type_id IS NULL AND p.event_work_type_id IS NOT NULL")) {
      fromProfile.executeUpdate();
    }
    try (PreparedStatement fromBatch = connection.prepareStatement(
        "UPDATE competition_registration r "
            + "JOIN competition_roster_item i "
            + "  ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0 "
            + "JOIN competition_roster_import b "
            + "  ON b.id = i.batch_id AND b.tenant_id = r.tenant_id AND b.deleted = 0 "
            + "SET r.event_work_type_id = b.event_work_type_id "
            + "WHERE r.deleted = 0 AND r.event_work_type_id IS NULL AND b.event_work_type_id IS NOT NULL")) {
      fromBatch.executeUpdate();
    }
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
