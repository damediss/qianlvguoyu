ALTER TABLE certification_assessor
  ADD COLUMN identity_no VARCHAR(32) NULL COMMENT '身份证号' AFTER assessor_name,
  ADD COLUMN certificate_photo_file_id BIGINT NULL COMMENT '考评员证照片文件ID' AFTER certificate_no,
  ADD COLUMN certificate_photo_file_name VARCHAR(255) NULL COMMENT '考评员证照片文件名' AFTER certificate_photo_file_id;

CREATE TABLE certification_assessor_ledger_import (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  source_file_id BIGINT NOT NULL COMMENT '上传的台账文件ID',
  status VARCHAR(32) NOT NULL COMMENT 'queued/processing/completed/failed',
  total_count INT NOT NULL DEFAULT 0 COMMENT '台账有效行数',
  imported_count INT NOT NULL DEFAULT 0 COMMENT '新增考评员数',
  skipped_count INT NOT NULL DEFAULT 0 COMMENT '因姓名或证号已存在而跳过的数量',
  failed_count INT NOT NULL DEFAULT 0 COMMENT '格式校验失败数量',
  result_message VARCHAR(4000) NULL COMMENT '导入摘要与前20条失败说明',
  operator_id BIGINT NULL COMMENT '发起导入的操作人ID',
  started_at DATETIME NULL COMMENT '处理开始时间',
  completed_at DATETIME NULL COMMENT '处理完成时间',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_assessor_ledger_import_status (tenant_id, status, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考评员台账导入批次';
