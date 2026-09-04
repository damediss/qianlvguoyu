-- 申报补贴材料实例与版本（工作台 Phase A）

CREATE TABLE IF NOT EXISTS train_subsidy_material_instance (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  subsidy_id          BIGINT       NOT NULL,
  slot_code           VARCHAR(64)  NULL COMMENT '配方槽位，可空=自定义',
  template_id         BIGINT       NULL,
  material_type       VARCHAR(32)  NOT NULL COMMENT 'application_form/roster/summary/photo/certificate/other',
  title               VARCHAR(200) NOT NULL,
  status              VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/filled/confirmed/upload_only/from_class',
  current_version_id  BIGINT       NULL,
  required_flag       TINYINT      NOT NULL DEFAULT 0,
  sort_order          INT          NOT NULL DEFAULT 0,
  renderer            VARCHAR(32)  NULL,
  tenant_id           BIGINT       NOT NULL,
  deleted             TINYINT      NOT NULL DEFAULT 0,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tsmi_subsidy (subsidy_id, deleted, sort_order),
  KEY idx_tsmi_slot (subsidy_id, slot_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴材料实例';

CREATE TABLE IF NOT EXISTS train_subsidy_material_version (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  instance_id         BIGINT       NOT NULL,
  version_no          INT          NOT NULL,
  file_id             BIGINT       NULL,
  file_path           VARCHAR(512) NULL,
  file_name           VARCHAR(255) NULL,
  source              VARCHAR(32)  NOT NULL DEFAULT 'upload' COMMENT 'auto_fill/manual_edit/upload/class_sync/rollback',
  field_snapshot_json MEDIUMTEXT   NULL,
  confirmed           TINYINT      NOT NULL DEFAULT 0,
  confirmed_by        BIGINT       NULL,
  confirmed_at        DATETIME     NULL,
  remark              VARCHAR(500) NULL,
  created_by          BIGINT       NULL,
  tenant_id           BIGINT       NOT NULL,
  deleted             TINYINT      NOT NULL DEFAULT 0,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tsmv_instance (instance_id, deleted, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴材料版本';

ALTER TABLE train_subsidy_slot_file
  ADD COLUMN instance_id BIGINT NULL COMMENT '关联材料实例' AFTER slot_code,
  ADD COLUMN version_id BIGINT NULL COMMENT '关联材料版本' AFTER instance_id;
