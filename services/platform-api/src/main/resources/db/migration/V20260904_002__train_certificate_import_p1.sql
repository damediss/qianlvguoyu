-- 证书导入 P1：查询地址、等级归一、file_id、导入批次审计
ALTER TABLE train_certificate
  ADD COLUMN query_urls VARCHAR(1000) NULL COMMENT '证面查询地址原文 JSON 数组' AFTER evaluation_org,
  ADD COLUMN level_normalized VARCHAR(64) NULL COMMENT '等级五档归一（报表用，证面原文仍在 level）' AFTER level,
  ADD COLUMN file_id BIGINT NULL COMMENT '证书文件 ID' AFTER file_path;

CREATE TABLE IF NOT EXISTS train_certificate_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  operator_id BIGINT NULL COMMENT '操作人',
  class_ids_json VARCHAR(1000) NULL COMMENT '勾选班级 ID JSON',
  preview_id VARCHAR(64) NULL COMMENT '预览 ID',
  total_files INT NULL DEFAULT 0 COMMENT '预览文件数',
  matched INT NULL DEFAULT 0,
  written INT NULL DEFAULT 0 COMMENT '新建证书行数',
  updated INT NULL DEFAULT 0 COMMENT '更新证书行数',
  skipped INT NULL DEFAULT 0,
  errors_json TEXT NULL COMMENT '跳过/错误摘要 JSON',
  remark VARCHAR(500) NULL,
  create_time DATETIME NULL,
  INDEX idx_cert_import_batch_tenant (tenant_id),
  INDEX idx_cert_import_batch_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='毕业典礼证书文件夹导入批次';
