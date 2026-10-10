-- ============================================================
-- 培训管理 → 战略规划 (pxzhan_gui_hua) 下新增「企业调研」「方案规划」
-- ============================================================

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000001001, 1, 2073936153367261186, 'page', 'pxyan_jiu',        '企业调研', '/pxzhan-gui-hua/research', 'training/TrainingPlaceholder', 'pxyanjiu:view',       'BulbOutlined',     1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (207393809000001002, 1, 2073936153367261186, 'page', 'pxan_fang_gui_hua', '方案规划', '/pxzhan-gui-hua/solution',  'training/TrainingPlaceholder', 'pxanfangguihua:view', 'SolutionOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000101001, 1, 207393809000001001, 'pxyanjiu:view',       '查看企业调研', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (207393809000101002, 1, 207393809000001002, 'pxanfangguihua:view',  '查看方案规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000201001, 1, 2000, 207393809000001001, NOW(), NOW(), 1, 1, 0),
  (207393809000201002, 1, 2000, 207393809000001002, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000301001, 1, 2000, 207393809000101001, NOW(), NOW(), 1, 1, 0),
  (207393809000301002, 1, 2000, 207393809000101002, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
