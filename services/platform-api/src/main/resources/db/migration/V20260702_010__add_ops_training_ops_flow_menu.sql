-- ============================================================
-- 运营流程 → 培训运营流程 菜单树
-- 与业务销售流程（9001）同级，挂在运营流程（9000）下
-- ============================================================

-- 1. 培训运营流程 目录 (parent_id=9000, sort_order=3)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9050, 1, 9000, 'directory', 'ops_training_ops_flow', '培训运营流程', '/ops-flow/training-ops', NULL, 'ops.training_ops_flow:view', 'ScheduleOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 培训运营流程子菜单 (parent_id=9050)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9051, 1, 9050, 'page', 'ops_train_demand_survey',  '需求调研', '/ops-flow/training-ops/demand-survey',  'product/ProductPlaceholder', 'ops.train_demand_survey:view',  'SearchOutlined',           1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9052, 1, 9050, 'page', 'ops_train_solution_design','方案设计', '/ops-flow/training-ops/solution-design','product/ProductPlaceholder', 'ops.train_solution_design:view','FormOutlined',             2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9053, 1, 9050, 'page', 'ops_train_class_mgmt',     '开班管理', '/ops-flow/training-ops/class-mgmt',     'product/ProductPlaceholder', 'ops.train_class_mgmt:view',     'TeamOutlined',             3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9054, 1, 9050, 'page', 'ops_train_implementation', '培训实施', '/ops-flow/training-ops/implementation', 'product/ProductPlaceholder', 'ops.train_implementation:view', 'PlayCircleOutlined',       4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9055, 1, 9050, 'page', 'ops_train_cert_issue',     '证书颁发', '/ops-flow/training-ops/cert-issue',     'product/ProductPlaceholder', 'ops.train_cert_issue:view',     'SafetyCertificateOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9056, 1, 9050, 'page', 'ops_train_graduation',     '毕业典礼', '/ops-flow/training-ops/graduation',     'product/ProductPlaceholder', 'ops.train_graduation:view',     'TrophyOutlined',           6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9057, 1, 9050, 'page', 'ops_train_project_close',  '项目结题', '/ops-flow/training-ops/project-close',  'product/ProductPlaceholder', 'ops.train_project_close:view',  'CheckCircleOutlined',      7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9058, 1, 9050, 'page', 'ops_train_project_apply',  '项目申报', '/ops-flow/training-ops/project-apply',  'product/ProductPlaceholder', 'ops.train_project_apply:view',  'FileAddOutlined',          8, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9150, 1, 9050, 'ops.training_ops_flow:view',      '查看培训运营流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9151, 1, 9051, 'ops.train_demand_survey:view',      '查看需求调研',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9152, 1, 9052, 'ops.train_solution_design:view',  '查看方案设计',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9153, 1, 9053, 'ops.train_class_mgmt:view',         '查看开班管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9154, 1, 9054, 'ops.train_implementation:view',     '查看培训实施',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9155, 1, 9055, 'ops.train_cert_issue:view',         '查看证书颁发',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9156, 1, 9056, 'ops.train_graduation:view',         '查看毕业典礼',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9157, 1, 9057, 'ops.train_project_close:view',      '查看项目结题',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9158, 1, 9058, 'ops.train_project_apply:view',      '查看项目申报',     'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9050 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9051, 1 UNION ALL SELECT 9052, 1 UNION ALL SELECT 9053, 1 UNION ALL
  SELECT 9054, 1 UNION ALL SELECT 9055, 1 UNION ALL SELECT 9056, 1 UNION ALL
  SELECT 9057, 1 UNION ALL SELECT 9058, 1
) t;

-- 5. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9150 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9151, 1 UNION ALL SELECT 9152, 1 UNION ALL SELECT 9153, 1 UNION ALL
  SELECT 9154, 1 UNION ALL SELECT 9155, 1 UNION ALL SELECT 9156, 1 UNION ALL
  SELECT 9157, 1 UNION ALL SELECT 9158, 1
) t;