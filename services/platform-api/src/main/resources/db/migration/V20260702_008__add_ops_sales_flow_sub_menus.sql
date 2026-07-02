-- ============================================================
-- 运营流程 → 业务销售流程 子菜单
-- 挂在业务销售流程（9001）下
-- ============================================================

-- 1. 订单财务 目录 (parent_id=9001, sort_order=3, 排在客户管理之后)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9020, 1, 9001, 'directory', 'ops_order_finance', '订单财务', '/ops-flow/sales/order-finance', NULL, 'ops.order_finance:view', 'DollarOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 营销推广 目录 (parent_id=9001, sort_order=4)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9021, 1, 9001, 'directory', 'ops_marketing', '营销推广', '/ops-flow/sales/marketing', NULL, 'ops.marketing:view', 'ThunderboltOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 数据看板 目录 (parent_id=9001, sort_order=5)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9022, 1, 9001, 'directory', 'ops_dashboard', '数据看板', '/ops-flow/sales/dashboard', NULL, 'ops.dashboard:view', 'DashboardOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 订单财务子菜单 (parent_id=9020)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9023, 1, 9020, 'page', 'ops_order_center',     '订单中心（2期）', '/ops-flow/sales/order-finance/order',     'product/ProductPlaceholder', 'ops.order_center:view',     'ShopOutlined',       1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9024, 1, 9020, 'page', 'ops_contract_mgmt',    '合同管理（2期）', '/ops-flow/sales/order-finance/contract',  'product/ProductPlaceholder', 'ops.contract_mgmt:view',    'FileTextOutlined',   2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9025, 1, 9020, 'page', 'ops_receipt_record',   '收款记录（2期）', '/ops-flow/sales/order-finance/receipt',   'product/ProductPlaceholder', 'ops.receipt_record:view',   'AccountBookOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9026, 1, 9020, 'page', 'ops_invoice_mgmt',     '开票管理（2期）', '/ops-flow/sales/order-finance/invoice',   'product/ProductPlaceholder', 'ops.invoice_mgmt:view',     'FileProtectOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9027, 1, 9020, 'page', 'ops_settlement',       '对账结算（2期）', '/ops-flow/sales/order-finance/settlement','product/ProductPlaceholder', 'ops.settlement:view',       'AuditOutlined',       5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 营销推广子菜单 (parent_id=9021)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9028, 1, 9021, 'page', 'ops_enroll_brochure',  '招生简章（2期）', '/ops-flow/sales/marketing/brochure',  'product/ProductPlaceholder', 'ops.enroll_brochure:view',  'FileTextOutlined',   1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9029, 1, 9021, 'page', 'ops_activity_mkt',     '活动营销（2期）', '/ops-flow/sales/marketing/activity',  'product/ProductPlaceholder', 'ops.activity_mkt:view',     'ThunderboltOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9030, 1, 9021, 'page', 'ops_coupon_card',      '优惠卡券（2期）', '/ops-flow/sales/marketing/coupon',    'product/ProductPlaceholder', 'ops.coupon_card:view',      'GiftOutlined',        3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9031, 1, 9021, 'page', 'ops_channel_qrcode',   '渠道活码（2期）', '/ops-flow/sales/marketing/qrcode',    'product/ProductPlaceholder', 'ops.channel_qrcode:view',   'QrcodeOutlined',      4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9032, 1, 9021, 'page', 'ops_distribution',     '分销裂变（2期）', '/ops-flow/sales/marketing/distribution','product/ProductPlaceholder', 'ops.distribution:view',     'ShareAltOutlined',    5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 数据看板子菜单 (parent_id=9022)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9033, 1, 9022, 'page', 'ops_dashboard_funnel',  '销售漏斗', '/ops-flow/sales/dashboard/funnel',  'product/ProductPlaceholder', 'ops.dashboard_funnel:view',  'FunnelPlotOutlined',  1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9034, 1, 9022, 'page', 'ops_dashboard_rank',    '招生排行', '/ops-flow/sales/dashboard/rank',    'product/ProductPlaceholder', 'ops.dashboard_rank:view',    'TrophyOutlined',      2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9035, 1, 9022, 'page', 'ops_dashboard_convert', '转化分析', '/ops-flow/sales/dashboard/convert', 'product/ProductPlaceholder', 'ops.dashboard_convert:view', 'LineChartOutlined',   3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9036, 1, 9022, 'page', 'ops_dashboard_target',  '业绩目标', '/ops-flow/sales/dashboard/target',  'product/ProductPlaceholder', 'ops.dashboard_target:view',  'AimOutlined',         4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9037, 1, 9022, 'page', 'ops_dashboard_roi',     '投入产出', '/ops-flow/sales/dashboard/roi',     'product/ProductPlaceholder', 'ops.dashboard_roi:view',     'FundOutlined',        5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 7. 权限
-- ============================================================
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9120, 1, 9020, 'ops.order_finance:view',       '查看订单财务',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9121, 1, 9021, 'ops.marketing:view',           '查看营销推广',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9122, 1, 9022, 'ops.dashboard:view',           '查看数据看板',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9123, 1, 9023, 'ops.order_center:view',        '查看订单中心',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9124, 1, 9024, 'ops.contract_mgmt:view',       '查看合同管理',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9125, 1, 9025, 'ops.receipt_record:view',      '查看收款记录',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9126, 1, 9026, 'ops.invoice_mgmt:view',        '查看开票管理',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9127, 1, 9027, 'ops.settlement:view',          '查看对账结算',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9128, 1, 9028, 'ops.enroll_brochure:view',     '查看招生简章',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9129, 1, 9029, 'ops.activity_mkt:view',        '查看活动营销',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9130, 1, 9030, 'ops.coupon_card:view',         '查看优惠卡券',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9131, 1, 9031, 'ops.channel_qrcode:view',      '查看渠道活码',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9132, 1, 9032, 'ops.distribution:view',        '查看分销裂变',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9133, 1, 9033, 'ops.dashboard_funnel:view',    '查看销售漏斗',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9134, 1, 9034, 'ops.dashboard_rank:view',      '查看招生排行',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9135, 1, 9035, 'ops.dashboard_convert:view',   '查看转化分析',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9136, 1, 9036, 'ops.dashboard_target:view',    '查看业绩目标',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9137, 1, 9037, 'ops.dashboard_roi:view',       '查看投入产出',       'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 8. 角色-菜单关联 (平台管理员 role_id=2000)
-- ============================================================
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9020 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9021, 1 UNION ALL SELECT 9022, 1 UNION ALL SELECT 9023, 1 UNION ALL
  SELECT 9024, 1 UNION ALL SELECT 9025, 1 UNION ALL SELECT 9026, 1 UNION ALL
  SELECT 9027, 1 UNION ALL SELECT 9028, 1 UNION ALL SELECT 9029, 1 UNION ALL
  SELECT 9030, 1 UNION ALL SELECT 9031, 1 UNION ALL SELECT 9032, 1 UNION ALL
  SELECT 9033, 1 UNION ALL SELECT 9034, 1 UNION ALL SELECT 9035, 1 UNION ALL
  SELECT 9036, 1 UNION ALL SELECT 9037, 1
) t;

-- ============================================================
-- 9. 角色-权限关联 (平台管理员 role_id=2000)
-- ============================================================
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9120 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9121, 1 UNION ALL SELECT 9122, 1 UNION ALL SELECT 9123, 1 UNION ALL
  SELECT 9124, 1 UNION ALL SELECT 9125, 1 UNION ALL SELECT 9126, 1 UNION ALL
  SELECT 9127, 1 UNION ALL SELECT 9128, 1 UNION ALL SELECT 9129, 1 UNION ALL
  SELECT 9130, 1 UNION ALL SELECT 9131, 1 UNION ALL SELECT 9132, 1 UNION ALL
  SELECT 9133, 1 UNION ALL SELECT 9134, 1 UNION ALL SELECT 9135, 1 UNION ALL
  SELECT 9136, 1 UNION ALL SELECT 9137, 1
) t;