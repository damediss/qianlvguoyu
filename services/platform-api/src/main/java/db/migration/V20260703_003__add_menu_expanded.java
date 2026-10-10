package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** 为 sys_menu 增加 expanded 字段，兼容列已存在的环境 */
public class V20260703_003__add_menu_expanded extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    if (!columnExists(conn, "sys_menu", "expanded")) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(
            "ALTER TABLE sys_menu "
                + "ADD COLUMN expanded TINYINT NOT NULL DEFAULT 1 "
                + "COMMENT '目录默认展开(1展开 0收起)' AFTER visible");
      }
    }
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("UPDATE sys_menu SET expanded = 1 WHERE expanded IS NULL");
    }
  }

  private static boolean columnExists(Connection conn, String table, String column) throws Exception {
    DatabaseMetaData meta = conn.getMetaData();
    String catalog = conn.getCatalog();
    try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
      if (rs.next()) {
        return true;
      }
    }
    try (ResultSet rs = meta.getColumns(catalog, null, table.toUpperCase(), column)) {
      return rs.next();
    }
  }
}
