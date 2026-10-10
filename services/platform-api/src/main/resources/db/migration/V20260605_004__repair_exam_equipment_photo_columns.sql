-- Repair equipment photo columns skipped by a historical Flyway version conflict.
-- 空库幂等化（踩坑 #34 修复）：空库中该表由下游 V20260605_010 才建出，此处表不存在即跳过；
-- 存量库表已存在，列缺失才补（与原语义一致）。存量库本版本已应用不会重跑。
SET @db := DATABASE();
SET @tbl := (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_exam_equipment');

SET @add_equipment_photo_file_id = (
  SELECT IF(
    @tbl > 0 AND COUNT(*) = 0,
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
    @tbl > 0 AND COUNT(*) = 0,
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
