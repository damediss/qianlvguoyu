-- V20260814_001: 跟班督导检查 / 整改闭环 / 满意度答卷
-- 《跟班日志与督导实现方案》L5 + M4 指标口径支撑

CREATE TABLE IF NOT EXISTS train_follow_supervision (
  id                 BIGINT       NOT NULL COMMENT '雪花 ID',
  tenant_id          BIGINT       NOT NULL,
  class_id           BIGINT       NOT NULL,
  biz_date           VARCHAR(16)  NOT NULL COMMENT 'YYYY-MM-DD',
  schedule_id        BIGINT       NULL,
  inspector_user_id  BIGINT       NULL,
  inspector_name     VARCHAR(64)  NULL,
  status             VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/submitted/closed',
  conclusion         VARCHAR(32)  NULL COMMENT 'pass/fail/conditional',
  remark             VARCHAR(1000) NULL,
  submitted_at       DATETIME     NULL,
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfs_class_date (class_id, biz_date, deleted),
  KEY idx_tfs_tenant (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟班督导检查单';

CREATE TABLE IF NOT EXISTS train_follow_supervision_item (
  id                 BIGINT       NOT NULL,
  tenant_id          BIGINT       NOT NULL,
  supervision_id     BIGINT       NOT NULL,
  item_code          VARCHAR(64)  NOT NULL,
  item_title         VARCHAR(200) NOT NULL,
  sort_no            INT          NOT NULL DEFAULT 0,
  result             VARCHAR(16)  NULL COMMENT 'pass/fail/na',
  remark             VARCHAR(500) NULL,
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfsi_sup (supervision_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='督导检查项';

CREATE TABLE IF NOT EXISTS train_follow_issue (
  id                 BIGINT       NOT NULL,
  tenant_id          BIGINT       NOT NULL,
  class_id           BIGINT       NOT NULL,
  supervision_id     BIGINT       NULL,
  item_id            BIGINT       NULL,
  title              VARCHAR(200) NOT NULL,
  description        VARCHAR(2000) NULL,
  severity           VARCHAR(16)  NOT NULL DEFAULT 'warn' COMMENT 'info/warn/critical',
  status             VARCHAR(16)  NOT NULL DEFAULT 'open' COMMENT 'open/doing/done/closed',
  assignee_name      VARCHAR(64)  NULL,
  due_date           VARCHAR(16)  NULL,
  resolve_note       VARCHAR(1000) NULL,
  opened_by          BIGINT       NULL,
  opened_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at          DATETIME     NULL,
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfi_class_status (class_id, status, deleted),
  KEY idx_tfi_sup (supervision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟班异常整改单';

CREATE TABLE IF NOT EXISTS train_follow_survey_response (
  id                 BIGINT       NOT NULL,
  tenant_id          BIGINT       NOT NULL,
  class_id           BIGINT       NOT NULL,
  biz_date           VARCHAR(16)  NULL,
  respondent_name    VARCHAR(64)  NULL,
  score              DECIMAL(4,1) NOT NULL COMMENT '1.0～5.0',
  comment_text       VARCHAR(1000) NULL,
  source             VARCHAR(32)  NOT NULL DEFAULT 'admin' COMMENT 'admin/app/import',
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfsr_class (class_id, deleted),
  KEY idx_tfsr_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟班满意度答卷';
