-- ============================================================
-- 战略规划 → 业务规划 子菜单
-- 挂在业务规划（8030）下，共 13 个部门工作规划
-- ============================================================

-- 1. 13 个业务规划子菜单 (parent_id=8030)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8031, 1, 8030, 'page', 'biz_product_design',    '产品设计部工作规划',    '/strate/business-planning/product-design',     'product/ProductPlaceholder', 'biz.product_design:view',    'SketchOutlined',       1,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8032, 1, 8030, 'page', 'biz_market_expand',     '市场拓展部工作规划',    '/strate/business-planning/market-expand',      'product/ProductPlaceholder', 'biz.market_expand:view',     'AimOutlined',          2,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8033, 1, 8030, 'page', 'biz_course_dev',        '课程开发部工作规划',    '/strate/business-planning/course-dev',         'product/ProductPlaceholder', 'biz.course_dev:view',        'BookOutlined',         3,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8034, 1, 8030, 'page', 'biz_agent_dev',         '智能体开发部工作规划',  '/strate/business-planning/agent-dev',          'product/ProductPlaceholder', 'biz.agent_dev:view',         'RobotOutlined',        4,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8035, 1, 8030, 'page', 'biz_cloud_dev',         '云平台开发部工作规划',  '/strate/business-planning/cloud-dev',          'product/ProductPlaceholder', 'biz.cloud_dev:view',         'CloudOutlined',        5,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8036, 1, 8030, 'page', 'biz_simulation_dev',    '仿真开发部工作规划',    '/strate/business-planning/simulation-dev',     'product/ProductPlaceholder', 'biz.simulation_dev:view',    'ExperimentOutlined',   6,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8037, 1, 8030, 'page', 'biz_training_ops',      '培训运营工作规划',      '/strate/business-planning/training-ops',       'product/ProductPlaceholder', 'biz.training_ops:view',      'ScheduleOutlined',     7,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8038, 1, 8030, 'page', 'biz_cert_ops',          '认定运营工作规划',      '/strate/business-planning/cert-ops',           'product/ProductPlaceholder', 'biz.cert_ops:view',          'SafetyCertificateOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8039, 1, 8030, 'page', 'biz_competition_ops',   '竞赛运营工作规划',      '/strate/business-planning/competition-ops',    'product/ProductPlaceholder', 'biz.competition_ops:view',   'TrophyOutlined',       9,  1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8040, 1, 8030, 'page', 'biz_hr',                '人力资源工作规划',      '/strate/business-planning/hr',                 'product/ProductPlaceholder', 'biz.hr:view',                'TeamOutlined',         10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8041, 1, 8030, 'page', 'biz_finance',           '业财融工作规划',        '/strate/business-planning/finance',            'product/ProductPlaceholder', 'biz.finance:view',           'DollarOutlined',       11, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8042, 1, 8030, 'page', 'biz_admin',             '行政后勤工作规划',      '/strate/business-planning/admin',              'product/ProductPlaceholder', 'biz.admin:view',             'HomeOutlined',         12, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8043, 1, 8030, 'page', 'biz_platform_coop',     '平台共建部工作规划',    '/strate/business-planning/platform-coop',      'product/ProductPlaceholder', 'biz.platform_coop:view',     'ApartmentOutlined',    13, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 权限 (每个菜单一条 view 权限)
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8131, 1, 8031, 'biz.product_design:view',    '查看产品设计部工作规划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8132, 1, 8032, 'biz.market_expand:view',     '查看市场拓展部工作规划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8133, 1, 8033, 'biz.course_dev:view',        '查看课程开发部工作规划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8134, 1, 8034, 'biz.agent_dev:view',         '查看智能体开发部工作规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8135, 1, 8035, 'biz.cloud_dev:view',         '查看云平台开发部工作规划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8136, 1, 8036, 'biz.simulation_dev:view',    '查看仿真开发部工作规划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8137, 1, 8037, 'biz.training_ops:view',      '查看培训运营工作规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8138, 1, 8038, 'biz.cert_ops:view',          '查看认定运营工作规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8139, 1, 8039, 'biz.competition_ops:view',   '查看竞赛运营工作规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8140, 1, 8040, 'biz.hr:view',                '查看人力资源工作规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8141, 1, 8041, 'biz.finance:view',           '查看业财融工作规划',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8142, 1, 8042, 'biz.admin:view',             '查看行政后勤工作规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8143, 1, 8043, 'biz.platform_coop:view',     '查看平台共建部工作规划',   'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 80000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8031 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 8032, 1 UNION ALL SELECT 8033, 1 UNION ALL SELECT 8034, 1 UNION ALL
  SELECT 8035, 1 UNION ALL SELECT 8036, 1 UNION ALL SELECT 8037, 1 UNION ALL
  SELECT 8038, 1 UNION ALL SELECT 8039, 1 UNION ALL SELECT 8040, 1 UNION ALL
  SELECT 8041, 1 UNION ALL SELECT 8042, 1 UNION ALL SELECT 8043, 1
) t;

-- 4. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 81000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8131 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 8132, 1 UNION ALL SELECT 8133, 1 UNION ALL SELECT 8134, 1 UNION ALL
  SELECT 8135, 1 UNION ALL SELECT 8136, 1 UNION ALL SELECT 8137, 1 UNION ALL
  SELECT 8138, 1 UNION ALL SELECT 8139, 1 UNION ALL SELECT 8140, 1 UNION ALL
  SELECT 8141, 1 UNION ALL SELECT 8142, 1 UNION ALL SELECT 8143, 1
) t;