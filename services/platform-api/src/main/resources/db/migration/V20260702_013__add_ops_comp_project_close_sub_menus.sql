-- ============================================================
-- 竞赛运营流程 → 竞赛结题 子菜单
-- 将竞赛结题（9111）改为 directory，并添加 3 个子页面
-- ============================================================

-- 1. 竞赛结题改为 directory
UPDATE sys_menu
SET menu_type = 'directory', component = NULL, updated_at = NOW(), updated_by = 1
WHERE id = 9111 AND tenant_id = 1 AND deleted = 0;

-- 2. 竞赛结题子菜单 (parent_id=9111)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9120, 1, 9111, 'page', 'ops_comp_close_report',     '报告', '/ops-flow/competition-ops/project-close/report',     'product/ProductPlaceholder', 'ops.comp_close_report:view',     'FileTextOutlined',  1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9121, 1, 9111, 'page', 'ops_comp_close_settlement', '结算', '/ops-flow/competition-ops/project-close/settlement', 'product/ProductPlaceholder', 'ops.comp_close_settlement:view', 'AccountBookOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9122, 1, 9111, 'page', 'ops_comp_close_apply',      '申报', '/ops-flow/competition-ops/project-close/apply',      'product/ProductPlaceholder', 'ops.comp_close_apply:view',      'FormOutlined',      3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9211, 1, 9120, 'ops.comp_close_report:view',     '查看报告', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9212, 1, 9121, 'ops.comp_close_settlement:view', '查看结算', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9213, 1, 9122, 'ops.comp_close_apply:view',      '查看申报', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (900120, 1, 2000, 9120, NOW(), NOW(), 1, 1, 0),
  (900121, 1, 2000, 9121, NOW(), NOW(), 1, 1, 0),
  (900122, 1, 2000, 9122, NOW(), NOW(), 1, 1, 0);

-- 5. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (910211, 1, 2000, 9211, NOW(), NOW(), 1, 1, 0),
  (910212, 1, 2000, 9212, NOW(), NOW(), 1, 1, 0),
  (910213, 1, 2000, 9213, NOW(), NOW(), 1, 1, 0);
