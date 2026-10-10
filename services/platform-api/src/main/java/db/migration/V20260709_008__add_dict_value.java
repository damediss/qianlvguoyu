package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 为 sys_dict 增加 value 字段。
 * 使用 Java 迁移实现幂等添加，兼容 Druid Wall（禁止 PREPARE 动态 SQL）。
 */
public class V20260709_008__add_dict_value extends BaseJavaMigration {

  private static final String TABLE_NAME = "sys_dict";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "value",
          "ALTER TABLE " + TABLE_NAME
              + " ADD COLUMN value VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '字典值' AFTER name"
      );
    }
  }

  private void addColumnIfMissing(
      Connection connection,
      Statement statement,
      String columnName,
      String ddl
  ) throws SQLException {
    if (!columnExists(connection, columnName)) {
      statement.execute(ddl);
    }
  }

  private boolean columnExists(Connection connection, String columnName) throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(),
        null,
        TABLE_NAME,
        columnName
    )) {
      return columns.next();
    }
  }
}
