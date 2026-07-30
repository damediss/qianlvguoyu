ALTER TABLE certification_assessor
  ADD COLUMN level_json JSON NULL COMMENT '结构化等级范围' AFTER level_scope,
  ADD COLUMN masked_phone VARCHAR(32) NULL COMMENT '脱敏电话' AFTER phone,
  ADD COLUMN org_id BIGINT NULL COMMENT '所属机构id' AFTER masked_phone,
  ADD COLUMN org_name VARCHAR(160) NULL COMMENT '所属机构名称' AFTER org_id,
  ADD COLUMN materials_description VARCHAR(1000) NULL COMMENT '材料说明' AFTER org_name,
  ADD COLUMN audit_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT '审核状态' AFTER materials_description,
  ADD COLUMN work_status VARCHAR(40) NOT NULL DEFAULT 'idle' COMMENT '在岗状态' AFTER audit_status,
  ADD COLUMN submitted_at DATETIME NULL COMMENT '提交审核时间' AFTER work_status,
  ADD COLUMN submitted_by BIGINT NULL COMMENT '提交人' AFTER submitted_at,
  ADD COLUMN reviewed_at DATETIME NULL COMMENT '审核时间' AFTER submitted_by,
  ADD COLUMN reviewed_by BIGINT NULL COMMENT '审核人' AFTER reviewed_at,
  ADD COLUMN reviewer_name VARCHAR(80) NULL COMMENT '审核人姓名' AFTER reviewed_by,
  ADD COLUMN reject_reason VARCHAR(1000) NULL COMMENT '最近驳回原因' AFTER reviewer_name,
  ADD COLUMN disabled_at DATETIME NULL COMMENT '停用时间' AFTER reject_reason,
  ADD COLUMN disabled_by BIGINT NULL COMMENT '停用人' AFTER disabled_at,
  ADD COLUMN disable_reason VARCHAR(500) NULL COMMENT '停用原因' AFTER disabled_by,
  ADD COLUMN referenced_count INT NOT NULL DEFAULT 0 COMMENT '有效引用次数' AFTER disable_reason;

ALTER TABLE certification_exam_site
  ADD COLUMN masked_contact_phone VARCHAR(32) NULL COMMENT '脱敏联系电话' AFTER contact_phone,
  ADD COLUMN materials_description VARCHAR(1000) NULL COMMENT '备案材料说明' AFTER equipment_condition,
  ADD COLUMN audit_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT '审核状态' AFTER materials_description,
  ADD COLUMN submitted_at DATETIME NULL COMMENT '提交审核时间' AFTER audit_status,
  ADD COLUMN submitted_by BIGINT NULL COMMENT '提交人' AFTER submitted_at,
  ADD COLUMN reviewed_at DATETIME NULL COMMENT '审核时间' AFTER submitted_by,
  ADD COLUMN reviewed_by BIGINT NULL COMMENT '审核人' AFTER reviewed_at,
  ADD COLUMN reviewer_name VARCHAR(80) NULL COMMENT '审核人姓名' AFTER reviewed_by,
  ADD COLUMN reject_reason VARCHAR(1000) NULL COMMENT '最近驳回原因' AFTER reviewer_name,
  ADD COLUMN disabled_at DATETIME NULL COMMENT '停用时间' AFTER reject_reason,
  ADD COLUMN disabled_by BIGINT NULL COMMENT '停用人' AFTER disabled_at,
  ADD COLUMN disable_reason VARCHAR(500) NULL COMMENT '停用原因' AFTER disabled_by,
  ADD COLUMN referenced_count INT NOT NULL DEFAULT 0 COMMENT '有效引用次数' AFTER disable_reason;

CREATE TABLE IF NOT EXISTS certification_exam_equipment (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  equipment_type VARCHAR(120) NOT NULL COMMENT '设备类型',
  asset_no VARCHAR(120) NOT NULL COMMENT '资产编号',
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

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6310, 1, 6300, 'directory', 'certification_info', '信息管理', '/certification/info', NULL, 'certification.info', 'DatabaseOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (6311, 1, 6310, 'page', 'certification_info_assessor', '考评员信息', '/certification/info/assessors', 'certification/info/AssessorInfoPage', 'certification.info.assessor.view', 'IdcardOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (6312, 1, 6310, 'page', 'certification_info_site', '考场信息', '/certification/info/sites', 'certification/info/ExamSiteInfoPage', 'certification.info.site.view', 'BankOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (6313, 1, 6310, 'page', 'certification_info_equipment', '考试设备', '/certification/info/equipment', 'certification/info/ExamEquipmentInfoPage', 'certification.info.equipment.view', 'ToolOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6930, 1, 6310, 'certification.info', '信息管理入口', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6931, 1, 6311, 'certification.info.assessor.view', '查看考评员信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6932, 1, 6311, 'certification.info.assessor.create', '新增考评员信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6933, 1, 6311, 'certification.info.assessor.update', '编辑考评员信息', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6934, 1, 6311, 'certification.info.assessor.delete', '删除考评员信息', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6935, 1, 6311, 'certification.info.assessor.submit', '提交考评员审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6936, 1, 6311, 'certification.info.assessor.review', '审核考评员信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6937, 1, 6311, 'certification.info.assessor.export', '导出考评员信息', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6938, 1, 6312, 'certification.info.site.view', '查看考场信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6939, 1, 6312, 'certification.info.site.create', '新增考场信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6940, 1, 6312, 'certification.info.site.update', '编辑考场信息', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6941, 1, 6312, 'certification.info.site.delete', '删除考场信息', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6942, 1, 6312, 'certification.info.site.submit', '提交考场审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6943, 1, 6312, 'certification.info.site.review', '审核考场信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6944, 1, 6312, 'certification.info.site.export', '导出考场信息', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6945, 1, 6313, 'certification.info.equipment.view', '查看考试设备', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6946, 1, 6313, 'certification.info.equipment.create', '新增考试设备', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6947, 1, 6313, 'certification.info.equipment.update', '编辑考试设备', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6948, 1, 6313, 'certification.info.equipment.delete', '删除考试设备', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6949, 1, 6313, 'certification.info.equipment.submit', '提交考试设备审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6950, 1, 6313, 'certification.info.equipment.review', '审核考试设备', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6951, 1, 6313, 'certification.info.equipment.export', '导出考试设备', 'export', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806310, 1, 2000, 6310, NOW(), NOW(), 1, 1, 0),
  (806311, 1, 2000, 6311, NOW(), NOW(), 1, 1, 0),
  (806312, 1, 2000, 6312, NOW(), NOW(), 1, 1, 0),
  (806313, 1, 2000, 6313, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (906930, 1, 2000, 6930, NOW(), NOW(), 1, 1, 0),
  (906931, 1, 2000, 6931, NOW(), NOW(), 1, 1, 0),
  (906932, 1, 2000, 6932, NOW(), NOW(), 1, 1, 0),
  (906933, 1, 2000, 6933, NOW(), NOW(), 1, 1, 0),
  (906934, 1, 2000, 6934, NOW(), NOW(), 1, 1, 0),
  (906935, 1, 2000, 6935, NOW(), NOW(), 1, 1, 0),
  (906936, 1, 2000, 6936, NOW(), NOW(), 1, 1, 0),
  (906937, 1, 2000, 6937, NOW(), NOW(), 1, 1, 0),
  (906938, 1, 2000, 6938, NOW(), NOW(), 1, 1, 0),
  (906939, 1, 2000, 6939, NOW(), NOW(), 1, 1, 0),
  (906940, 1, 2000, 6940, NOW(), NOW(), 1, 1, 0),
  (906941, 1, 2000, 6941, NOW(), NOW(), 1, 1, 0),
  (906942, 1, 2000, 6942, NOW(), NOW(), 1, 1, 0),
  (906943, 1, 2000, 6943, NOW(), NOW(), 1, 1, 0),
  (906944, 1, 2000, 6944, NOW(), NOW(), 1, 1, 0),
  (906945, 1, 2000, 6945, NOW(), NOW(), 1, 1, 0),
  (906946, 1, 2000, 6946, NOW(), NOW(), 1, 1, 0),
  (906947, 1, 2000, 6947, NOW(), NOW(), 1, 1, 0),
  (906948, 1, 2000, 6948, NOW(), NOW(), 1, 1, 0),
  (906949, 1, 2000, 6949, NOW(), NOW(), 1, 1, 0),
  (906950, 1, 2000, 6950, NOW(), NOW(), 1, 1, 0),
  (906951, 1, 2000, 6951, NOW(), NOW(), 1, 1, 0);
