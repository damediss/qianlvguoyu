package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20260703_002__add_fingerprint_trace_image_reference extends BaseJavaMigration {

  private static final String TABLE_NAME = "certification_practical_fingerprint_trace";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      addColumnIfMissing(
          connection,
          statement,
          "fingerprint_image_file_id",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN fingerprint_image_file_id BIGINT NULL AFTER collected_at"
      );
      addColumnIfMissing(
          connection,
          statement,
          "fingerprint_image_hash",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN fingerprint_image_hash VARCHAR(128) NULL AFTER fingerprint_image_file_id"
      );
      addColumnIfMissing(
          connection,
          statement,
          "fingerprint_image_format",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN fingerprint_image_format VARCHAR(16) NULL AFTER fingerprint_image_hash"
      );
      addColumnIfMissing(
          connection,
          statement,
          "fingerprint_image_captured_at",
          "ALTER TABLE " + TABLE_NAME + " ADD COLUMN fingerprint_image_captured_at DATETIME NULL AFTER fingerprint_image_format"
      );
      if (!indexExists(connection, "idx_cert_practical_fingerprint_image_file")) {
        statement.execute(
            "ALTER TABLE " + TABLE_NAME
                + " ADD KEY idx_cert_practical_fingerprint_image_file "
                + "(tenant_id, fingerprint_image_file_id, deleted)"
        );
      }
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

  private boolean indexExists(Connection connection, String indexName) throws SQLException {
    try (ResultSet indexes = connection.getMetaData().getIndexInfo(
        connection.getCatalog(),
        null,
        TABLE_NAME,
        false,
        false
    )) {
      while (indexes.next()) {
        if (indexName.equals(indexes.getString("INDEX_NAME"))) {
          return true;
        }
      }
      return false;
    }
  }
}
