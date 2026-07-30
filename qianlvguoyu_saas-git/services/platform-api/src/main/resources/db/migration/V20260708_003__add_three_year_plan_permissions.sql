-- 方案规划（三年规划）权限扩展

UPDATE sys_menu
SET component = 'training/SolutionPlanning',
    updated_at = NOW(),
    updated_by = 1
WHERE code = 'pxan_fang_gui_hua'
  AND deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000101003, 1, 207393809000001002, 'pxanfangguihua:manage',             '管理方案规划', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (207393809000101004, 1, 207393809000001002, 'pxanfangguihua:enterprise_confirm', '企业确认方案规划', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000301003, 1, 2000, 207393809000101003, NOW(), NOW(), 1, 1, 0),
  (207393809000301004, 1, 2000, 207393809000101004, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
