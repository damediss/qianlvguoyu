package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 分班一期：企业回传列（替换空操作语义）+ train_info_list.three_year_plan_id（幂等）。
 */
public class V20260716_001__division_phase1_columns extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_roster_file_id",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_roster_file_id BIGINT NULL COMMENT '企业回传：筛选名单文件ID'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_shift_file_id",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_shift_file_id BIGINT NULL COMMENT '企业回传：排班表文件ID'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_roster_file_name",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_roster_file_name VARCHAR(255) NULL COMMENT '企业回传名单原始文件名'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_shift_file_name",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_shift_file_name VARCHAR(255) NULL COMMENT '企业回传排班表原始文件名'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_return_time",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_return_time DATETIME NULL COMMENT '企业最近回传时间'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_return_user_id",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_return_user_id BIGINT NULL COMMENT '企业最近回传人ID'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_return_username",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_return_username VARCHAR(64) NULL COMMENT '企业最近回传人'");
      addColumnIfMissing(
          connection,
          statement,
          "train_three_year_plan",
          "enterprise_return_history_json",
          "ALTER TABLE train_three_year_plan ADD COLUMN enterprise_return_history_json MEDIUMTEXT NULL COMMENT '企业回传历史JSON'");

      addColumnIfMissing(
          connection,
          statement,
          "train_info_list",
          "three_year_plan_id",
          "ALTER TABLE train_info_list ADD COLUMN three_year_plan_id BIGINT NULL COMMENT '来源三年规划ID' AFTER traindemand_id");
      if (!indexExists(connection, "train_info_list", "idx_til_plan")) {
        statement.execute(
            "CREATE INDEX idx_til_plan ON train_info_list (three_year_plan_id, deleted)");
      }
    }
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

  private boolean indexExists(Connection connection, String tableName, String indexName)
      throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(),
        null,
        tableName,
        false,
        false
    )) {
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
