INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6320, 1, 6300, 'page', 'certification_pre_exam', '认定前', '/certification/pre-exam', 'certification/PreExam', 'certification.pre_exam.view', 'SafetyCertificateOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (6330, 1, 6300, 'directory', 'certification_info', '信息管理', '/certification/info', NULL, 'certification.info', 'DatabaseOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  code = VALUES(code),
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
SET
  parent_id = 6330,
  sort_order = CASE code
    WHEN 'certification_info_assessor' THEN 1
    WHEN 'certification_info_site' THEN 2
    WHEN 'certification_info_equipment' THEN 3
    ELSE sort_order
  END,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code IN ('certification_info_assessor', 'certification_info_site', 'certification_info_equipment');

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6960, 1, 6330, 'certification.info', '信息管理入口', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6961, 1, 6311, 'certification.info.assessor.view', '查看考评员信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6962, 1, 6311, 'certification.info.assessor.create', '新增考评员信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6963, 1, 6311, 'certification.info.assessor.update', '编辑考评员信息', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6964, 1, 6311, 'certification.info.assessor.delete', '删除考评员信息', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6965, 1, 6311, 'certification.info.assessor.submit', '提交考评员审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6966, 1, 6311, 'certification.info.assessor.review', '审核考评员信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6967, 1, 6311, 'certification.info.assessor.export', '导出考评员信息', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6968, 1, 6312, 'certification.info.site.view', '查看考场信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6969, 1, 6312, 'certification.info.site.create', '新增考场信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6970, 1, 6312, 'certification.info.site.update', '编辑考场信息', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6971, 1, 6312, 'certification.info.site.delete', '删除考场信息', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6972, 1, 6312, 'certification.info.site.submit', '提交考场审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6973, 1, 6312, 'certification.info.site.review', '审核考场信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6974, 1, 6312, 'certification.info.site.export', '导出考场信息', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6975, 1, 6313, 'certification.info.equipment.view', '查看考试设备', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6976, 1, 6313, 'certification.info.equipment.create', '新增考试设备', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6977, 1, 6313, 'certification.info.equipment.update', '编辑考试设备', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6978, 1, 6313, 'certification.info.equipment.delete', '删除考试设备', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6979, 1, 6313, 'certification.info.equipment.submit', '提交考试设备审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6980, 1, 6313, 'certification.info.equipment.review', '审核考试设备', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6981, 1, 6313, 'certification.info.equipment.export', '导出考试设备', 'export', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6982, 1, 6320, 'certification.pre_exam.view', '查看认定前', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6983, 1, 6320, 'certification.pre_exam.material.edit', '编辑认定前材料', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6984, 1, 6320, 'certification.pre_exam.material.review', '复核认定前材料', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6985, 1, 6320, 'certification.pre_exam.equipment.edit', '编辑认定前设备', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6986, 1, 6320, 'certification.pre_exam.equipment.review', '复核认定前设备', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6987, 1, 6320, 'certification.pre_exam.staff.edit', '编辑认定前人员', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6988, 1, 6320, 'certification.pre_exam.staff.review', '复核认定前人员', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6989, 1, 6320, 'certification.pre_exam.complete', '完成认定前阶段', 'complete', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806330, 1, 2000, 6330, NOW(), NOW(), 1, 1, 0),
  (999103, 1, 2604, 6320, NOW(), NOW(), 1, 1, 0),
  (999104, 1, 2604, 6330, NOW(), NOW(), 1, 1, 0),
  (999105, 1, 2604, 6311, NOW(), NOW(), 1, 1, 0),
  (999106, 1, 2604, 6312, NOW(), NOW(), 1, 1, 0),
  (999107, 1, 2604, 6313, NOW(), NOW(), 1, 1, 0),
  (999108, 1, 2000, 6320, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909800 + p.id - 6982, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.pre_exam.view',
    'certification.pre_exam.material.edit',
    'certification.pre_exam.material.review',
    'certification.pre_exam.equipment.edit',
    'certification.pre_exam.equipment.review',
    'certification.pre_exam.staff.edit',
    'certification.pre_exam.staff.review',
    'certification.pre_exam.complete'
  );

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909850 + p.id - 6982, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.pre_exam.view',
    'certification.pre_exam.material.edit',
    'certification.pre_exam.material.review',
    'certification.pre_exam.equipment.edit',
    'certification.pre_exam.equipment.review',
    'certification.pre_exam.staff.edit',
    'certification.pre_exam.staff.review',
    'certification.pre_exam.complete'
  );

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909600 + p.id - 6960, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.info%';

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909700 + p.id - 6960, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.info%';

