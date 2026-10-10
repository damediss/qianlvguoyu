package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 为 train_sign_up_people_self 增加 cert_verified 查证状态字段（幂等）。
 */
public class V20260714_001__add_cert_verified_to_sign_up_people extends BaseJavaMigration {

  private static final String TABLE_NAME = "train_sign_up_people_self";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "cert_verified",
          "ALTER TABLE " + TABLE_NAME
              + " ADD COLUMN cert_verified TINYINT NULL DEFAULT NULL"
              + " COMMENT '证书查证状态：null未查证 0查证中 1已查证 2查证无结果 3查证失败'"
              + " AFTER level"
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
