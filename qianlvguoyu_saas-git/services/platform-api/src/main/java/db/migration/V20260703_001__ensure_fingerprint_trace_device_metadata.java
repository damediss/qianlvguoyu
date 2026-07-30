package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20260703_001__ensure_fingerprint_trace_device_metadata extends BaseJavaMigration {

  private static final String TABLE_NAME = "certification_practical_fingerprint_trace";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "device_vendor_id",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN device_vendor_id VARCHAR(32) NULL AFTER device_model"
      );
      addColumnIfMissing(
          connection,
          statement,
          "device_product_id",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN device_product_id VARCHAR(32) NULL AFTER device_vendor_id"
      );
      addColumnIfMissing(
          connection,
          statement,
          "sdk_device_type",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN sdk_device_type VARCHAR(64) NULL AFTER device_product_id"
      );
      addColumnIfMissing(
          connection,
          statement,
          "duration_ms",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN duration_ms BIGINT NULL AFTER quality_score"
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
