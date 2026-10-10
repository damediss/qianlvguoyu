-- sys_post: position management table
CREATE TABLE IF NOT EXISTS sys_post (
  id         BIGINT       NOT NULL PRIMARY KEY,
  tenant_id  BIGINT       NOT NULL,
  code       VARCHAR(64)  NOT NULL COMMENT 'Position code',
  name       VARCHAR(128) NOT NULL COMMENT 'Position name',
  sort_order INT          NOT NULL DEFAULT 0 COMMENT 'Sort order',
  status     VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT 'Status',
  remark     VARCHAR(512) NULL COMMENT 'Remark',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT       NULL,
  updated_by BIGINT       NULL,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_post_tenant_code (tenant_id, code, deleted),
  KEY idx_sys_post_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add post_id to sys_user (idempotent)
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'post_id');
SET @sql := IF(@exist = 0,
  'ALTER TABLE sys_user ADD COLUMN post_id BIGINT NULL AFTER org_id, ADD KEY idx_sys_user_post (tenant_id, post_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
