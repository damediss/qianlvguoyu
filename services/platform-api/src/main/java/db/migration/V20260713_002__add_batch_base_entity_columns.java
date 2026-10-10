package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 为查证批次表补充 BaseEntity 字段，幂等添加以兼容 Druid Wall 与重复执行场景。
 */
public class V20260713_002__add_batch_base_entity_columns extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addBatchColumns(connection, statement, "chazheng_cert_batch");
      addBatchColumns(connection, statement, "chazheng_cert_batch_item");
    }
  }

  private void addBatchColumns(Connection connection, Statement statement, String tableName)
      throws SQLException {
    addColumnIfMissing(
        connection,
        statement,
        tableName,
        "tenant_id",
        "ALTER TABLE " + tableName + " ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户 ID'"
    );
    addColumnIfMissing(
        connection,
        statement,
        tableName,
        "created_by",
        "ALTER TABLE " + tableName + " ADD COLUMN created_by BIGINT NULL COMMENT '创建人'"
    );
    addColumnIfMissing(
        connection,
        statement,
        tableName,
        "updated_by",
        "ALTER TABLE " + tableName + " ADD COLUMN updated_by BIGINT NULL COMMENT '更新人'"
    );
  }

  private void addColumnIfMissing(
      Connection connection,
      Statement statement,
      String tableName,
      String columnName,
      String ddl
  ) throws SQLException {
    if (!columnExists(connection, tableName, columnName)) {
      statement.execute(ddl);
    }
  }

  private boolean columnExists(Connection connection, String tableName, String columnName)
      throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(
        connection.getCatalog(),
        null,
        tableName,
        columnName
    )) {
      return columns.next();
    }
  }
}
