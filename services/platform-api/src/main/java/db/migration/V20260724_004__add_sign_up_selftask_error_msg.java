package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 遗留 train_sign_up_selftask 可能缺少 error_msg / tenant_id。
 * <p>
 * 不能用 SQL + PREPARE（Druid Wall 会拦截），改用 JDBC 幂等补列。
 */
public class V20260724_004__add_sign_up_selftask_error_msg extends BaseJavaMigration {

  private static final String TABLE_NAME = "train_sign_up_selftask";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "error_msg",
          "ALTER TABLE " + TABLE_NAME
              + " ADD COLUMN error_msg VARCHAR(500) NULL COMMENT '失败信息' AFTER endtime");
      addColumnIfMissing(
          connection,
          statement,
          "tenant_id",
          "ALTER TABLE " + TABLE_NAME
              + " ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER error_msg");
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
