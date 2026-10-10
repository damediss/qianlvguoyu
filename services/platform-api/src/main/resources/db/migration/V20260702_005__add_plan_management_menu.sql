-- ============================================================
-- 战略规划 → 计划管理 目录
-- 与人才标准（8001）同级，挂在战略规划（8000）下
-- ============================================================

-- 1. 计划管理 目录 (parent_id=8000, sort_order=6, 排在人才规划之后)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8060, 1, 8000, 'directory', 'strate_plan_mgmt', '计划管理', '/strate/plan-mgmt', NULL, 'strate.plan_mgmt:view', 'CalendarOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8160, 1, 8060, 'strate.plan_mgmt:view', '查看计划管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800060, 1, 2000, 8060, NOW(), NOW(), 1, 1, 0);

-- 4. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (810160, 1, 2000, 8160, NOW(), NOW(), 1, 1, 0);