-- 教材生成管理：菜单 + 权限码 + 角色授权（职业标准驱动教材生成一期 §2.5）
-- 权限三步链：菜单（第 1 段）→ 权限码（第 2 段）→ sys_role_menu + sys_role_permission 授权（第 3 段）
-- 缺第三步会让接口 403，联调前先确认本脚本已执行。
--
-- 取号（2026-10-21 双库核查）：两库 MAX(version) 均为 20261020.004；本迁移取 20261021.002（与 V20261021_001 同批）。
-- 注：本文件曾用号 20260821.002（乱序车道），verifier 复核时改名，测试库 flyway_schema_history
--     对应行 version 已同步更新为 20261021.002。
-- tenant_id 一律 10159（千铝工投运行租户）；全部幂等（先查后插 / ON DUPLICATE KEY UPDATE）。
-- 挂载点：智能课程材料管理目录（parent_id=2087736644369305601，两库均存在），
-- 该目录下现有子菜单最大 sort_order=4（含资源管理），本菜单取 5。

SET @tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @parent_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND id = 2087736644369305601 AND deleted = 0
  ORDER BY id LIMIT 1
), 2087736644369305601);

-- 1) 菜单：教材生成管理
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @textbook_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_project' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @textbook_menu_id, @tenant_id, @parent_menu_id, 'menu', 'textbook_project', '教材生成管理',
  '/jiao-cai-sheng-cheng', 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookProjectPage',
  'textbook:project:view',
  'BookOutlined', 5, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @textbook_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_project' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- 2) 权限码：查看 / 管理 / 导出
SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @textbook_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:project:view' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @textbook_manage_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:project:manage' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @textbook_export_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:project:export' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@textbook_view_permission_id, @tenant_id, @textbook_menu_id,
   'textbook:project:view', '查看教材工程', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@textbook_manage_permission_id, @tenant_id, @textbook_menu_id,
   'textbook:project:manage', '管理教材工程', 'manage', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@textbook_export_permission_id, @tenant_id, @textbook_menu_id,
   'textbook:project:export', '导出教材目录', 'export', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- 3) 角色授权：平台管理员（role_id=2000）挂菜单 + 三个权限码
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @tenant_id, 2000, @textbook_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM DUAL
WHERE @textbook_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = 2000
      AND existing.menu_id = @textbook_menu_id
      AND existing.deleted = 0
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @tenant_id, 2000, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_permission p
WHERE p.tenant_id = @tenant_id
  AND p.code IN ('textbook:project:view', 'textbook:project:manage', 'textbook:project:export')
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = 2000
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );
