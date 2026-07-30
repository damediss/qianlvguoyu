-- ============================================================
-- 启用「培训仪表盘」菜单 (menu id=4929, code=train_dashboard)
-- 该菜单的 status/deleted 在历史 seed 后被误置,导致前端侧栏不显示
-- 同时确保平台管理员(2000)拥有 sys_role_menu 关联
-- ============================================================

UPDATE sys_menu
   SET status = 'enabled', deleted = 0, visible = 1, sort_order = 1
 WHERE id = 4929;

UPDATE sys_role_menu
   SET deleted = 0
 WHERE menu_id = 4929 AND role_id = 2000;

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT 8002929, 1, 2000, 4929, NOW(), NOW(), 1, 1, 0
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2000 AND menu_id = 4929);
