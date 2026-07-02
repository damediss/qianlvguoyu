-- ============================================================
-- 战略规划 → 计划管理 子菜单
-- 挂在计划管理（8060）下
-- ============================================================

-- 1. 工作计划 目录 (parent_id=8060, sort_order=1)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8061, 1, 8060, 'directory', 'plan_work', '工作计划', '/strate/plan-mgmt/work', NULL, 'plan.work:view', 'ScheduleOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 培训计划 目录 (parent_id=8060, sort_order=2)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8062, 1, 8060, 'directory', 'plan_training', '培训计划', '/strate/plan-mgmt/training', NULL, 'plan.training:view', 'BookOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 工作计划子菜单 (parent_id=8061)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8063, 1, 8061, 'page', 'plan_work_year',   '年度计划', '/strate/plan-mgmt/work/year',   'product/ProductPlaceholder', 'plan.work_year:view',   'CalendarOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8064, 1, 8061, 'page', 'plan_work_quarter', '季度计划', '/strate/plan-mgmt/work/quarter', 'product/ProductPlaceholder', 'plan.work_quarter:view', 'CalendarOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8065, 1, 8061, 'page', 'plan_work_month',   '月度计划', '/strate/plan-mgmt/work/month',   'product/ProductPlaceholder', 'plan.work_month:view',   'CalendarOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 培训计划子菜单 (parent_id=8062)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8066, 1, 8062, 'page', 'plan_train_year',   '年度计划', '/strate/plan-mgmt/training/year',   'product/ProductPlaceholder', 'plan.train_year:view',   'CalendarOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8067, 1, 8062, 'page', 'plan_train_quarter', '季度计划', '/strate/plan-mgmt/training/quarter', 'product/ProductPlaceholder', 'plan.train_quarter:view', 'CalendarOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8068, 1, 8062, 'page', 'plan_train_month',   '月度计划', '/strate/plan-mgmt/training/month',   'product/ProductPlaceholder', 'plan.train_month:view',   'CalendarOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8161, 1, 8061, 'plan.work:view',          '查看工作计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8162, 1, 8062, 'plan.training:view',       '查看培训计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8163, 1, 8063, 'plan.work_year:view',      '查看年度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8164, 1, 8064, 'plan.work_quarter:view',   '查看季度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8165, 1, 8065, 'plan.work_month:view',     '查看月度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8166, 1, 8066, 'plan.train_year:view',     '查看年度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8167, 1, 8067, 'plan.train_quarter:view',  '查看季度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8168, 1, 8068, 'plan.train_month:view',    '查看月度计划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 80000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8061 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 8062, 1 UNION ALL SELECT 8063, 1 UNION ALL SELECT 8064, 1 UNION ALL
  SELECT 8065, 1 UNION ALL SELECT 8066, 1 UNION ALL SELECT 8067, 1 UNION ALL
  SELECT 8068, 1
) t;

-- 7. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 81000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8161 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 8162, 1 UNION ALL SELECT 8163, 1 UNION ALL SELECT 8164, 1 UNION ALL
  SELECT 8165, 1 UNION ALL SELECT 8166, 1 UNION ALL SELECT 8167, 1 UNION ALL
  SELECT 8168, 1
) t;