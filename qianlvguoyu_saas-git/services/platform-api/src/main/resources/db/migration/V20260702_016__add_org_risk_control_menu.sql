-- ============================================================
-- 机构风控 顶层菜单树
-- ============================================================

-- 1. 机构风控 顶层目录
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9600, 1, 0, 'directory', 'org_risk_control', '机构风控', '/risk-control', NULL, 'risk:view', 'SecurityScanOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 各部门风控页面
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9601, 1, 9600, 'page', 'risk_product_design',  '产品设计部风控',   '/risk-control/product-design',  'product/ProductPlaceholder', 'risk.product_design:view',  'SketchOutlined',            1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9602, 1, 9600, 'page', 'risk_market_expand',   '市场拓展部风控',   '/risk-control/market-expand',   'product/ProductPlaceholder', 'risk.market_expand:view',   'AimOutlined',               2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9603, 1, 9600, 'page', 'risk_course_dev',      '课程开发部风控',   '/risk-control/course-dev',      'product/ProductPlaceholder', 'risk.course_dev:view',      'BookOutlined',              3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9604, 1, 9600, 'page', 'risk_agent_dev',       '智能体开发部风控', '/risk-control/agent-dev',       'product/ProductPlaceholder', 'risk.agent_dev:view',       'RobotOutlined',             4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9605, 1, 9600, 'page', 'risk_cloud_dev',       '云平台开发部风控', '/risk-control/cloud-dev',       'product/ProductPlaceholder', 'risk.cloud_dev:view',       'CloudOutlined',             5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9606, 1, 9600, 'page', 'risk_simulation_dev',  '仿真开发部风控',   '/risk-control/simulation-dev',  'product/ProductPlaceholder', 'risk.simulation_dev:view',  'ExperimentOutlined',        6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9607, 1, 9600, 'page', 'risk_training_ops',    '培训运营风控',     '/risk-control/training-ops',    'product/ProductPlaceholder', 'risk.training_ops:view',    'ScheduleOutlined',          7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9608, 1, 9600, 'page', 'risk_cert_ops',        '认定运营风控',     '/risk-control/cert-ops',        'product/ProductPlaceholder', 'risk.cert_ops:view',        'SafetyCertificateOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9609, 1, 9600, 'page', 'risk_competition_ops', '竞赛运营风控',     '/risk-control/competition-ops', 'product/ProductPlaceholder', 'risk.competition_ops:view', 'TrophyOutlined',            9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9610, 1, 9600, 'page', 'risk_hr',              '人力资源风控',     '/risk-control/hr',              'product/ProductPlaceholder', 'risk.hr:view',              'TeamOutlined',              10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9611, 1, 9600, 'page', 'risk_finance',         '业财融风控',       '/risk-control/finance',         'product/ProductPlaceholder', 'risk.finance:view',         'DollarOutlined',            11, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9612, 1, 9600, 'page', 'risk_admin',           '行政后勤风控',     '/risk-control/admin',           'product/ProductPlaceholder', 'risk.admin:view',           'HomeOutlined',              12, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9613, 1, 9600, 'page', 'risk_platform_coop',   '平台共建部风控',   '/risk-control/platform-coop',   'product/ProductPlaceholder', 'risk.platform_coop:view',   'ApartmentOutlined',         13, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9780, 1, 9600, 'risk:view',                 '查看机构风控',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9781, 1, 9601, 'risk.product_design:view',  '查看产品设计部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9782, 1, 9602, 'risk.market_expand:view',   '查看市场拓展部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9783, 1, 9603, 'risk.course_dev:view',      '查看课程开发部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9784, 1, 9604, 'risk.agent_dev:view',       '查看智能体开发部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9785, 1, 9605, 'risk.cloud_dev:view',       '查看云平台开发部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9786, 1, 9606, 'risk.simulation_dev:view',  '查看仿真开发部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9787, 1, 9607, 'risk.training_ops:view',    '查看培训运营风控',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9788, 1, 9608, 'risk.cert_ops:view',        '查看认定运营风控',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9789, 1, 9609, 'risk.competition_ops:view', '查看竞赛运营风控',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9790, 1, 9610, 'risk.hr:view',              '查看人力资源风控',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9791, 1, 9611, 'risk.finance:view',         '查看业财融风控',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9792, 1, 9612, 'risk.admin:view',           '查看行政后勤风控',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9793, 1, 9613, 'risk.platform_coop:view',   '查看平台共建部风控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 96000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE id BETWEEN 9600 AND 9613 AND tenant_id = 1 AND deleted = 0;

-- 5. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 97800 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE id BETWEEN 9780 AND 9793 AND tenant_id = 1 AND deleted = 0;
