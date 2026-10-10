-- ============================================================
-- 运营流程 顶层菜单树
-- ============================================================

-- 1. 运营流程 顶层目录 (parent_id=0, sort=7)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9000, 1, 0, 'directory', 'ops_flow', '运营流程', '/ops-flow', NULL, 'ops:view', 'NodeIndexOutlined', 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 业务销售流程 目录 (parent_id=9000)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9001, 1, 9000, 'directory', 'ops_sales_flow', '业务销售流程', '/ops-flow/sales', NULL, 'ops.sales:view', 'SwapOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 线索招生 目录 (parent_id=9001)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9002, 1, 9001, 'directory', 'ops_lead_enroll', '线索招生', '/ops-flow/sales/lead', NULL, 'ops.lead:view', 'UsergroupAddOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 线索招生子菜单 (parent_id=9002)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9003, 1, 9002, 'page', 'ops_lead_collect',  '线索收集', '/ops-flow/sales/lead/collect',  'product/ProductPlaceholder', 'ops.lead_collect:view',  'AimOutlined',        1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9004, 1, 9002, 'page', 'ops_lead_assign',   '线索分配', '/ops-flow/sales/lead/assign',   'product/ProductPlaceholder', 'ops.lead_assign:view',   'SendOutlined',       2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9005, 1, 9002, 'page', 'ops_lead_follow',   '客户跟进', '/ops-flow/sales/lead/follow',   'product/ProductPlaceholder', 'ops.lead_follow:view',   'CustomerServiceOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9006, 1, 9002, 'page', 'ops_lead_convert',  '报名转化', '/ops-flow/sales/lead/convert',  'product/ProductPlaceholder', 'ops.lead_convert:view',  'SwapOutlined',       4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 客户管理 目录 (parent_id=9001)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9007, 1, 9001, 'directory', 'ops_customer_mgmt', '客户管理', '/ops-flow/sales/customer', NULL, 'ops.customer:view', 'TeamOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 客户管理子菜单 (parent_id=9007)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9008, 1, 9007, 'page', 'ops_customer_archive',  '客户档案', '/ops-flow/sales/customer/archive',  'product/ProductPlaceholder', 'ops.customer_archive:view',  'IdcardOutlined',      1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9009, 1, 9007, 'page', 'ops_customer_tag',      '标签画像', '/ops-flow/sales/customer/tag',      'product/ProductPlaceholder', 'ops.customer_tag:view',      'TagOutlined',         2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9010, 1, 9007, 'page', 'ops_customer_intent',   '意向分级', '/ops-flow/sales/customer/intent',   'product/ProductPlaceholder', 'ops.customer_intent:view',   'StarOutlined',        3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9011, 1, 9007, 'page', 'ops_customer_visit',    '回访记录', '/ops-flow/sales/customer/visit',    'product/ProductPlaceholder', 'ops.customer_visit:view',    'HistoryOutlined',     4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9012, 1, 9007, 'page', 'ops_customer_remind',   '跟进提醒', '/ops-flow/sales/customer/remind',   'product/ProductPlaceholder', 'ops.customer_remind:view',   'BellOutlined',        5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 7. 权限
-- ============================================================
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9100, 1, 9000, 'ops:view',                '查看运营流程',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9101, 1, 9001, 'ops.sales:view',           '查看业务销售流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9102, 1, 9002, 'ops.lead:view',            '查看线索招生',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9103, 1, 9003, 'ops.lead_collect:view',    '查看线索收集',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9104, 1, 9004, 'ops.lead_assign:view',     '查看线索分配',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9105, 1, 9005, 'ops.lead_follow:view',     '查看客户跟进',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9106, 1, 9006, 'ops.lead_convert:view',    '查看报名转化',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9107, 1, 9007, 'ops.customer:view',        '查看客户管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9108, 1, 9008, 'ops.customer_archive:view', '查看客户档案',    'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9109, 1, 9009, 'ops.customer_tag:view',    '查看标签画像',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9110, 1, 9010, 'ops.customer_intent:view', '查看意向分级',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9111, 1, 9011, 'ops.customer_visit:view',  '查看回访记录',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9112, 1, 9012, 'ops.customer_remind:view', '查看跟进提醒',     'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 8. 角色-菜单关联 (平台管理员 role_id=2000)
-- ============================================================
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9000 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9001, 1 UNION ALL SELECT 9002, 1 UNION ALL SELECT 9003, 1 UNION ALL
  SELECT 9004, 1 UNION ALL SELECT 9005, 1 UNION ALL SELECT 9006, 1 UNION ALL
  SELECT 9007, 1 UNION ALL SELECT 9008, 1 UNION ALL SELECT 9009, 1 UNION ALL
  SELECT 9010, 1 UNION ALL SELECT 9011, 1 UNION ALL SELECT 9012, 1
) t;

-- ============================================================
-- 9. 角色-权限关联 (平台管理员 role_id=2000)
-- ============================================================
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9100 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9101, 1 UNION ALL SELECT 9102, 1 UNION ALL SELECT 9103, 1 UNION ALL
  SELECT 9104, 1 UNION ALL SELECT 9105, 1 UNION ALL SELECT 9106, 1 UNION ALL
  SELECT 9107, 1 UNION ALL SELECT 9108, 1 UNION ALL SELECT 9109, 1 UNION ALL
  SELECT 9110, 1 UNION ALL SELECT 9111, 1 UNION ALL SELECT 9112, 1
) t;