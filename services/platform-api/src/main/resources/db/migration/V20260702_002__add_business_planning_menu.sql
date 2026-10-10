-- ============================================================
-- 战略规划 → 业务规划 目录
-- 与产品规划（8003）同级，挂在战略规划（8000）下
-- ============================================================

-- 1. 业务规划 目录 (parent_id=8000, sort_order=4, 排在产品规划之后)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8030, 1, 8000, 'directory', 'strate_business_planning', '业务规划', '/strate/business-planning', NULL, 'strate.business:view', 'ClusterOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8130, 1, 8030, 'strate.business:view', '查看业务规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800030, 1, 2000, 8030, NOW(), NOW(), 1, 1, 0);

-- 4. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (810130, 1, 2000, 8130, NOW(), NOW(), 1, 1, 0);