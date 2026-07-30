CREATE TABLE IF NOT EXISTS certification_info_exam_staff (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  user_id BIGINT NULL COMMENT '平台用户id',
  staff_name VARCHAR(80) NOT NULL COMMENT '考务人员姓名',
  certificate_no VARCHAR(120) NOT NULL COMMENT '证号',
  work_type_scope VARCHAR(500) NOT NULL COMMENT '工种范围',
  work_type_json JSON NULL COMMENT '结构化工种范围',
  level_scope VARCHAR(300) NULL COMMENT '等级范围',
  level_json JSON NULL COMMENT '结构化等级范围',
  valid_until DATE NOT NULL COMMENT '有效期至',
  phone VARCHAR(32) NULL COMMENT '联系电话',
  masked_phone VARCHAR(32) NULL COMMENT '脱敏电话',
  org_id BIGINT NULL COMMENT '所属机构id',
  org_name VARCHAR(160) NULL COMMENT '所属机构名称',
  materials_description VARCHAR(1000) NULL COMMENT '材料说明',
  audit_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT '审核状态',
  work_status VARCHAR(40) NOT NULL DEFAULT 'idle' COMMENT '在岗状态',
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
  UNIQUE KEY uk_cert_info_exam_staff_cert (tenant_id, certificate_no, deleted),
  KEY idx_cert_info_exam_staff_audit (tenant_id, audit_status, deleted),
  KEY idx_cert_info_exam_staff_work (tenant_id, work_status, valid_until, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考务人员基础档案';

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6314, 1, 6330, 'page', 'certification_info_exam_staff', '考务信息', '/certification/info/exam-staff', 'certification/info/ExamStaffInfoPage', 'certification.info.exam-staff.view', 'TeamOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = VALUES(updated_by),
  deleted = VALUES(deleted);

UPDATE sys_menu
SET sort_order = CASE code
  WHEN 'certification_info_assessor' THEN 1
  WHEN 'certification_info_site' THEN 2
  WHEN 'certification_info_equipment' THEN 3
  WHEN 'certification_info_exam_staff' THEN 4
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND parent_id = 6330
  AND code IN ('certification_info_assessor', 'certification_info_site', 'certification_info_equipment', 'certification_info_exam_staff');

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6990, 1, 6314, 'certification.info.exam-staff.view', '查看考务信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6991, 1, 6314, 'certification.info.exam-staff.create', '新增考务信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6992, 1, 6314, 'certification.info.exam-staff.update', '编辑考务信息', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6993, 1, 6314, 'certification.info.exam-staff.delete', '删除考务信息', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6994, 1, 6314, 'certification.info.exam-staff.submit', '提交考务审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6995, 1, 6314, 'certification.info.exam-staff.review', '审核考务信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6996, 1, 6314, 'certification.info.exam-staff.export', '导出考务信息', 'export', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806314, 1, 2000, 6314, NOW(), NOW(), 1, 1, 0),
  (999109, 1, 2604, 6314, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909900 + p.id - 6990, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.id BETWEEN 6990 AND 6996;

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909920 + p.id - 6990, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.id BETWEEN 6990 AND 6996;
