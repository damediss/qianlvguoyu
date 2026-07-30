ALTER TABLE certification_archive_material
  DROP INDEX uk_cert_archive_material_type,
  ADD COLUMN active_material_type VARCHAR(80)
    GENERATED ALWAYS AS (
      CASE WHEN deleted = 0 THEN material_type ELSE NULL END
    ) STORED,
  ADD UNIQUE KEY uk_cert_archive_material_active (
    tenant_id,
    archive_id,
    active_material_type
  );
