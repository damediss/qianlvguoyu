-- 申报补贴材料模板库（Phase B：可复用填数模板）

CREATE TABLE IF NOT EXISTS train_subsidy_material_template (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  scope               VARCHAR(16)  NOT NULL DEFAULT 'tenant' COMMENT 'platform/tenant/class',
  tenant_id           BIGINT       NULL COMMENT '机构模板必填；platform 可空',
  class_id            BIGINT       NULL COMMENT '班级覆盖模板必填',
  material_type       VARCHAR(32)  NOT NULL DEFAULT 'application_form'
                      COMMENT 'application_form/roster/summary/photo/certificate/other',
  slot_code           VARCHAR(64)  NULL COMMENT '可选，关联配方槽位',
  region_code         VARCHAR(32)  NULL,
  train_mode          VARCHAR(32)  NULL,
  policy_year         INT          NULL,
  title               VARCHAR(200) NOT NULL,
  file_format         VARCHAR(16)  NOT NULL DEFAULT 'docx' COMMENT 'docx/xlsx',
  template_file_id    BIGINT       NOT NULL COMMENT 'sys_file.id',
  template_file_path  VARCHAR(512) NULL,
  field_schema_json   MEDIUMTEXT   NULL COMMENT '解析出的占位符 schema',
  parse_version       INT          NOT NULL DEFAULT 1,
  status              VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'draft/active/retired',
  remark              VARCHAR(500) NULL,
  created_by          BIGINT       NULL,
  updated_by          BIGINT       NULL,
  deleted             TINYINT      NOT NULL DEFAULT 0,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tsmt_scope (scope, tenant_id, material_type, status, deleted),
  KEY idx_tsmt_class (class_id, deleted),
  KEY idx_tsmt_slot (slot_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴材料填数模板库';
