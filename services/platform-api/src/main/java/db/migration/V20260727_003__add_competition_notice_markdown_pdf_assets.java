package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 竞赛公告支持 markdown/pdf，并创建受控资产表。
 * <p>
 * 不能用 SQL + PREPARE（Druid Wall 会拦截），改用 JDBC 幂等补齐。
 */
public class V20260727_003__add_competition_notice_markdown_pdf_assets extends BaseJavaMigration {

  private static final String NOTICE_TABLE = "competition_notice";
  private static final String ASSET_TABLE = "competition_notice_asset";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          NOTICE_TABLE,
          "content_type",
          "ALTER TABLE " + NOTICE_TABLE
              + " ADD COLUMN content_type VARCHAR(16) NOT NULL DEFAULT 'markdown' AFTER title");

      statement.execute(
          "ALTER TABLE " + NOTICE_TABLE + " MODIFY COLUMN content MEDIUMTEXT NOT NULL");

      addConstraintIfMissing(
          connection,
          statement,
          NOTICE_TABLE,
          "chk_competition_notice_content_type",
          "ALTER TABLE " + NOTICE_TABLE
              + " ADD CONSTRAINT chk_competition_notice_content_type"
              + " CHECK (content_type IN ('markdown', 'pdf'))");

      addIndexIfMissing(
          connection,
          statement,
          NOTICE_TABLE,
          "idx_competition_notice_content_type",
          "ALTER TABLE " + NOTICE_TABLE
              + " ADD KEY idx_competition_notice_content_type"
              + " (tenant_id, event_id, content_type, status, deleted)");

      if (!tableExists(connection, ASSET_TABLE)) {
        statement.execute(
            "CREATE TABLE " + ASSET_TABLE + " ("
                + "id BIGINT NOT NULL PRIMARY KEY,"
                + "tenant_id BIGINT NOT NULL,"
                + "notice_id BIGINT NOT NULL,"
                + "file_id BIGINT NOT NULL,"
                + "asset_type VARCHAR(32) NOT NULL,"
                + "sort_order INT NOT NULL DEFAULT 0,"
                + "deleted TINYINT NOT NULL DEFAULT 0,"
                + "created_by BIGINT NULL,"
                + "updated_by BIGINT NULL,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL,"
                + "CONSTRAINT chk_competition_notice_asset_type"
                + " CHECK (asset_type IN ('inline_image', 'pdf')),"
                + "UNIQUE KEY uk_competition_notice_asset_file"
                + " (tenant_id, notice_id, file_id, deleted),"
                + "KEY idx_competition_notice_asset_notice"
                + " (tenant_id, notice_id, asset_type, sort_order, deleted),"
                + "KEY idx_competition_notice_asset_file"
                + " (tenant_id, file_id, deleted)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
                + " COMMENT='竞赛公告受控资产'");
      }
    }
  }

  private void addColumnIfMissing(
      Connection connection, Statement statement, String table, String column, String ddl)
      throws SQLException {
    if (!columnExists(connection, table, column)) {
      statement.execute(ddl);
    }
  }

  private void addConstraintIfMissing(
      Connection connection, Statement statement, String table, String constraint, String ddl)
      throws SQLException {
    if (!constraintExists(connection, table, constraint)) {
      statement.execute(ddl);
    }
  }

  private void addIndexIfMissing(
      Connection connection, Statement statement, String table, String index, String ddl)
      throws SQLException {
    if (!indexExists(connection, table, index)) {
      statement.execute(ddl);
    }
  }

  private boolean tableExists(Connection connection, String table) throws SQLException {
    try (ResultSet tables = connection.getMetaData().getTables(
        connection.getCatalog(), null, table, new String[] {"TABLE"})) {
      return tables.next();
    }
  }

  private boolean columnExists(Connection connection, String table, String column)
      throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(), null, table, column)) {
      return columns.next();
    }
  }

  private boolean constraintExists(Connection connection, String table, String constraint)
      throws SQLException {
    String sql =
        "SELECT 1 FROM information_schema.TABLE_CONSTRAINTS"
            + " WHERE TABLE_SCHEMA = DATABASE()"
            + " AND TABLE_NAME = ?"
            + " AND CONSTRAINT_NAME = ?"
            + " LIMIT 1";
    try (var ps = connection.prepareStatement(sql)) {
      ps.setString(1, table);
      ps.setString(2, constraint);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean indexExists(Connection connection, String table, String index)
      throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(), null, table, false, false)) {
      while (indexes.next()) {
        String name = indexes.getString("INDEX_NAME");
        if (index.equalsIgnoreCase(name)) {
          return true;
        }
      }
      return false;
    }
  }
}
