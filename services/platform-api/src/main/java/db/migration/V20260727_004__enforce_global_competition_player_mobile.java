package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 竞赛选手手机号全局唯一（跨租户、仅未删除记录）。
 * <p>
 * 不能用 CREATE PROCEDURE / SIGNAL（Druid Wall 不支持），改用 JDBC 幂等迁移。
 */
public class V20260727_004__enforce_global_competition_player_mobile extends BaseJavaMigration {

  private static final String TABLE = "competition_player_identity";
  private static final String GENERATED_COLUMN = "active_mobile_lookup_hash";
  private static final String UNIQUE_INDEX = "uk_competition_player_identity_active_mobile_global";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    assertNoActiveMobileDuplicates(connection);

    try (Statement statement = connection.createStatement()) {
      if (!columnExists(connection, GENERATED_COLUMN)) {
        statement.execute(
            "ALTER TABLE " + TABLE
                + " ADD COLUMN " + GENERATED_COLUMN + " CHAR(64)"
                + " GENERATED ALWAYS AS ("
                + "CASE WHEN deleted = 0 THEN mobile_lookup_hash ELSE NULL END"
                + ") STORED");
      }

      if (!indexExists(connection, UNIQUE_INDEX)) {
        statement.execute(
            "ALTER TABLE " + TABLE
                + " ADD UNIQUE KEY " + UNIQUE_INDEX + " (" + GENERATED_COLUMN + ")");
      }
    }
  }

  private void assertNoActiveMobileDuplicates(Connection connection) throws SQLException {
    String sql =
        "SELECT mobile_lookup_hash, COUNT(*) AS cnt"
            + " FROM " + TABLE
            + " WHERE deleted = 0"
            + " GROUP BY mobile_lookup_hash"
            + " HAVING COUNT(*) > 1"
            + " LIMIT 1";
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      if (rs.next()) {
        throw new IllegalStateException(
            "Competition player mobile hash is duplicated across tenants; "
                + "resolve active duplicate phone numbers before migration. "
                + "sample_hash=" + rs.getString("mobile_lookup_hash")
                + ", count=" + rs.getLong("cnt"));
      }
    }
  }

  private boolean columnExists(Connection connection, String columnName) throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(), null, TABLE, columnName)) {
      return columns.next();
    }
  }

  private boolean indexExists(Connection connection, String indexName) throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(), null, TABLE, false, false)) {
      while (indexes.next()) {
        String name = indexes.getString("INDEX_NAME");
        if (indexName.equalsIgnoreCase(name)) {
          return true;
        }
      }
      return false;
    }
  }
}
