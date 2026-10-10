-- ============================================================
-- 战略规划 → 人才规划 目录 + 子菜单
-- 与产品规划（8003）、业务规划（8030）同级，挂在战略规划（8000）下
-- ============================================================

-- 1. 人才规划 目录 (parent_id=8000, sort_order=5, 排在业务规划之后)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8050, 1, 8000, 'directory', 'strate_talent_planning', '人才规划', '/strate/talent-planning', NULL, 'strate.talent:view', 'SolutionOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 三年规划
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8051, 1, 8050, 'page', 'talent_three_year', '三年规划', '/strate/talent-planning/three-year', 'product/ProductPlaceholder', 'talent.three_year:view', 'FieldTimeOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 五年规划
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8052, 1, 8050, 'page', 'talent_five_year', '五年规划', '/strate/talent-planning/five-year', 'product/ProductPlaceholder', 'talent.five_year:view', 'FieldTimeOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8150, 1, 8050, 'strate.talent:view',   '查看人才规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8151, 1, 8051, 'talent.three_year:view', '查看三年规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8152, 1, 8052, 'talent.five_year:view',  '查看五年规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800050, 1, 2000, 8050, NOW(), NOW(), 1, 1, 0),
  (800051, 1, 2000, 8051, NOW(), NOW(), 1, 1, 0),
  (800052, 1, 2000, 8052, NOW(), NOW(), 1, 1, 0);

-- 6. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (810150, 1, 2000, 8150, NOW(), NOW(), 1, 1, 0),
  (810151, 1, 2000, 8151, NOW(), NOW(), 1, 1, 0),
  (810152, 1, 2000, 8152, NOW(), NOW(), 1, 1, 0);