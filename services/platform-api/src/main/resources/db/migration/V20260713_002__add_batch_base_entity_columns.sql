-- 补充 BaseEntity 字段(tenant_id/created_by/updated_by)——适用于已执行 V20260713_001 的现有数据库
-- chazheng_cert_batch: 加 tenant_id
SET @db := DATABASE();
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE chazheng_cert_batch ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户 ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chazheng_cert_batch: 加 created_by
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE chazheng_cert_batch ADD COLUMN created_by BIGINT NULL COMMENT ''创建人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chazheng_cert_batch: 加 updated_by
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch' AND COLUMN_NAME = 'updated_by') = 0,
  'ALTER TABLE chazheng_cert_batch ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chazheng_cert_batch_item: 加 tenant_id
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch_item' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE chazheng_cert_batch_item ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户 ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chazheng_cert_batch_item: 加 created_by
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch_item' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE chazheng_cert_batch_item ADD COLUMN created_by BIGINT NULL COMMENT ''创建人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chazheng_cert_batch_item: 加 updated_by
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert_batch_item' AND COLUMN_NAME = 'updated_by') = 0,
  'ALTER TABLE chazheng_cert_batch_item ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
