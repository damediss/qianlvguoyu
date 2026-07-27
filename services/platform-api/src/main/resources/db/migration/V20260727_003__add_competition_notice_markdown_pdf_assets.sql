-- Requires the already-applied V20260724_001 migration to be present in the target Flyway history.
ALTER TABLE competition_notice
  ADD COLUMN content_type VARCHAR(16) NOT NULL DEFAULT 'markdown' AFTER title,
  MODIFY COLUMN content MEDIUMTEXT NOT NULL;

ALTER TABLE competition_notice
  ADD CONSTRAINT chk_competition_notice_content_type
    CHECK (content_type IN ('markdown', 'pdf')),
  ADD KEY idx_competition_notice_content_type
    (tenant_id, event_id, content_type, status, deleted);

CREATE TABLE competition_notice_asset (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  notice_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  asset_type VARCHAR(32) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT chk_competition_notice_asset_type
    CHECK (asset_type IN ('inline_image', 'pdf')),
  UNIQUE KEY uk_competition_notice_asset_file
    (tenant_id, notice_id, file_id, deleted),
  KEY idx_competition_notice_asset_notice
    (tenant_id, notice_id, asset_type, sort_order, deleted),
  KEY idx_competition_notice_asset_file
    (tenant_id, file_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛公告受控资产';
