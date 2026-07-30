-- Repair equipment photo columns skipped by a historical Flyway version conflict.
SET @add_equipment_photo_file_id = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_equipment ADD COLUMN equipment_photo_file_id BIGINT NULL COMMENT ''equipment photo file id'' AFTER equipment_status',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_equipment'
    AND COLUMN_NAME = 'equipment_photo_file_id'
);
PREPARE add_equipment_photo_file_id_stmt FROM @add_equipment_photo_file_id;
EXECUTE add_equipment_photo_file_id_stmt;
DEALLOCATE PREPARE add_equipment_photo_file_id_stmt;

SET @add_equipment_photo_file_name = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_equipment ADD COLUMN equipment_photo_file_name VARCHAR(300) NULL COMMENT ''equipment photo file name'' AFTER equipment_photo_file_id',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_equipment'
    AND COLUMN_NAME = 'equipment_photo_file_name'
);
PREPARE add_equipment_photo_file_name_stmt FROM @add_equipment_photo_file_name;
EXECUTE add_equipment_photo_file_name_stmt;
DEALLOCATE PREPARE add_equipment_photo_file_name_stmt;
