-- Certification info management schema.
-- Menu/permission seeds live in V20260605_012 (6330/6960+ ids avoid 6310/6930 collisions).

SET @add_assessor_level_json = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN level_json JSON NULL COMMENT ''结构化等级范围'' AFTER level_scope',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'level_json'
);
PREPARE stmt FROM @add_assessor_level_json;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_masked_phone = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN masked_phone VARCHAR(32) NULL COMMENT ''脱敏电话'' AFTER phone',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'masked_phone'
);
PREPARE stmt FROM @add_assessor_masked_phone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_org_id = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN org_id BIGINT NULL COMMENT ''所属机构id'' AFTER masked_phone',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'org_id'
);
PREPARE stmt FROM @add_assessor_org_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_org_name = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN org_name VARCHAR(160) NULL COMMENT ''所属机构名称'' AFTER org_id',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'org_name'
);
PREPARE stmt FROM @add_assessor_org_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_materials_description = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN materials_description VARCHAR(1000) NULL COMMENT ''材料说明'' AFTER org_name',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'materials_description'
);
PREPARE stmt FROM @add_assessor_materials_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_audit_status = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN audit_status VARCHAR(40) NOT NULL DEFAULT ''draft'' COMMENT ''审核状态'' AFTER materials_description',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'audit_status'
);
PREPARE stmt FROM @add_assessor_audit_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_work_status = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN work_status VARCHAR(40) NOT NULL DEFAULT ''idle'' COMMENT ''在岗状态'' AFTER audit_status',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'work_status'
);
PREPARE stmt FROM @add_assessor_work_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_submitted_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN submitted_at DATETIME NULL COMMENT ''提交审核时间'' AFTER work_status',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'submitted_at'
);
PREPARE stmt FROM @add_assessor_submitted_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_submitted_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN submitted_by BIGINT NULL COMMENT ''提交人'' AFTER submitted_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'submitted_by'
);
PREPARE stmt FROM @add_assessor_submitted_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_reviewed_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN reviewed_at DATETIME NULL COMMENT ''审核时间'' AFTER submitted_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'reviewed_at'
);
PREPARE stmt FROM @add_assessor_reviewed_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_reviewed_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN reviewed_by BIGINT NULL COMMENT ''审核人'' AFTER reviewed_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'reviewed_by'
);
PREPARE stmt FROM @add_assessor_reviewed_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_reviewer_name = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN reviewer_name VARCHAR(80) NULL COMMENT ''审核人姓名'' AFTER reviewed_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'reviewer_name'
);
PREPARE stmt FROM @add_assessor_reviewer_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_reject_reason = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN reject_reason VARCHAR(1000) NULL COMMENT ''最近驳回原因'' AFTER reviewer_name',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'reject_reason'
);
PREPARE stmt FROM @add_assessor_reject_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_disabled_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN disabled_at DATETIME NULL COMMENT ''停用时间'' AFTER reject_reason',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'disabled_at'
);
PREPARE stmt FROM @add_assessor_disabled_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_disabled_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN disabled_by BIGINT NULL COMMENT ''停用人'' AFTER disabled_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'disabled_by'
);
PREPARE stmt FROM @add_assessor_disabled_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_disable_reason = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN disable_reason VARCHAR(500) NULL COMMENT ''停用原因'' AFTER disabled_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'disable_reason'
);
PREPARE stmt FROM @add_assessor_disable_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_assessor_referenced_count = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_assessor ADD COLUMN referenced_count INT NOT NULL DEFAULT 0 COMMENT ''有效引用次数'' AFTER disable_reason',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_assessor'
    AND COLUMN_NAME = 'referenced_count'
);
PREPARE stmt FROM @add_assessor_referenced_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_masked_contact_phone = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN masked_contact_phone VARCHAR(32) NULL COMMENT ''脱敏联系电话'' AFTER contact_phone',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'masked_contact_phone'
);
PREPARE stmt FROM @add_site_masked_contact_phone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_materials_description = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN materials_description VARCHAR(1000) NULL COMMENT ''备案材料说明'' AFTER equipment_condition',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'materials_description'
);
PREPARE stmt FROM @add_site_materials_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_audit_status = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN audit_status VARCHAR(40) NOT NULL DEFAULT ''draft'' COMMENT ''审核状态'' AFTER materials_description',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'audit_status'
);
PREPARE stmt FROM @add_site_audit_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_submitted_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN submitted_at DATETIME NULL COMMENT ''提交审核时间'' AFTER audit_status',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'submitted_at'
);
PREPARE stmt FROM @add_site_submitted_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_submitted_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN submitted_by BIGINT NULL COMMENT ''提交人'' AFTER submitted_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'submitted_by'
);
PREPARE stmt FROM @add_site_submitted_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_reviewed_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN reviewed_at DATETIME NULL COMMENT ''审核时间'' AFTER submitted_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'reviewed_at'
);
PREPARE stmt FROM @add_site_reviewed_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_reviewed_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN reviewed_by BIGINT NULL COMMENT ''审核人'' AFTER reviewed_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'reviewed_by'
);
PREPARE stmt FROM @add_site_reviewed_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_reviewer_name = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN reviewer_name VARCHAR(80) NULL COMMENT ''审核人姓名'' AFTER reviewed_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'reviewer_name'
);
PREPARE stmt FROM @add_site_reviewer_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_reject_reason = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN reject_reason VARCHAR(1000) NULL COMMENT ''最近驳回原因'' AFTER reviewer_name',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'reject_reason'
);
PREPARE stmt FROM @add_site_reject_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_disabled_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN disabled_at DATETIME NULL COMMENT ''停用时间'' AFTER reject_reason',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'disabled_at'
);
PREPARE stmt FROM @add_site_disabled_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_disabled_by = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN disabled_by BIGINT NULL COMMENT ''停用人'' AFTER disabled_at',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'disabled_by'
);
PREPARE stmt FROM @add_site_disabled_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_disable_reason = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN disable_reason VARCHAR(500) NULL COMMENT ''停用原因'' AFTER disabled_by',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'disable_reason'
);
PREPARE stmt FROM @add_site_disable_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_site_referenced_count = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE certification_exam_site ADD COLUMN referenced_count INT NOT NULL DEFAULT 0 COMMENT ''有效引用次数'' AFTER disable_reason',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'certification_exam_site'
    AND COLUMN_NAME = 'referenced_count'
);
PREPARE stmt FROM @add_site_referenced_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS certification_exam_equipment (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  equipment_type VARCHAR(120) NOT NULL COMMENT '设备类型',
  asset_no VARCHAR(120) NOT NULL COMMENT '资产编号',
  -- 空库幂等化（踩坑 #34 修复）：V20260603_018 的 ADD COLUMN 在存量库已把该列加进 PHP 老表；
  -- 空库由本处建表，必须带上同一列，否则空库终态与存量库分叉
  equipment_quantity INT NOT NULL DEFAULT 1 COMMENT '设备数量',
  site_id BIGINT NULL COMMENT '所属考场id',
  site_name VARCHAR(200) NULL COMMENT '所属考点名称',
  equipment_model VARCHAR(160) NULL COMMENT '型号规格',
  calibration_valid_until DATE NULL COMMENT '校准有效期',
  equipment_status VARCHAR(40) NOT NULL DEFAULT 'normal' COMMENT '设备状态',
  materials_description VARCHAR(1000) NULL COMMENT '设备资料说明',
  audit_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT '审核状态',
  submitted_at DATETIME NULL COMMENT '提交审核时间',
  submitted_by BIGINT NULL COMMENT '提交人',
  reviewed_at DATETIME NULL COMMENT '审核时间',
  reviewed_by BIGINT NULL COMMENT '审核人',
  reviewer_name VARCHAR(80) NULL COMMENT '审核人姓名',
  reject_reason VARCHAR(1000) NULL COMMENT '最近驳回原因',
  disabled_at DATETIME NULL COMMENT '停用时间',
  disabled_by BIGINT NULL COMMENT '停用人',
  disable_reason VARCHAR(500) NULL COMMENT '停用原因',
  referenced_count INT NOT NULL DEFAULT 0 COMMENT '有效引用次数',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_exam_equipment_asset (tenant_id, asset_no, deleted),
  KEY idx_cert_exam_equipment_audit (tenant_id, audit_status, deleted),
  KEY idx_cert_exam_equipment_status (tenant_id, equipment_status, calibration_valid_until, deleted),
  KEY idx_cert_exam_equipment_site (tenant_id, site_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试设备基础档案';

CREATE TABLE IF NOT EXISTS certification_info_audit_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  owner_type VARCHAR(40) NOT NULL COMMENT 'assessor/exam_site/exam_equipment',
  owner_id BIGINT NOT NULL COMMENT '业务对象id',
  action VARCHAR(40) NOT NULL COMMENT 'submit/pass/reject/disable/delete',
  before_status VARCHAR(40) NULL COMMENT '操作前状态',
  after_status VARCHAR(40) NULL COMMENT '操作后状态',
  operator_id BIGINT NULL COMMENT '操作人id',
  operator_name VARCHAR(80) NULL COMMENT '操作人姓名',
  comment VARCHAR(1000) NULL COMMENT '意见或原因',
  snapshot_json JSON NULL COMMENT '对象快照',
  process_instance_id VARCHAR(128) NULL COMMENT '流程实例id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_info_audit_owner (tenant_id, owner_type, owner_id, created_at, deleted),
  KEY idx_cert_info_audit_operator (tenant_id, operator_id, created_at, deleted),
  KEY idx_cert_info_audit_action (tenant_id, owner_type, action, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信息管理审核记录';
