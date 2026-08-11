-- P2：受控 platform-file artifact 读取权限。
-- 不创建可见管理菜单；Bot 服务账号应由管理员单独绑定到最小角色。
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2084569000000000101, 1, NULL, 'file:artifact:read', '受控读取文件产物', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'file:artifact:read' AND deleted = 0
);

-- 仅为现有平台管理员提供本地冒烟/配置能力；生产 Bot 服务账号不应使用管理员账号。
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2300000000000000001, 1, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1 AND p.code = 'file:artifact:read' AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );
