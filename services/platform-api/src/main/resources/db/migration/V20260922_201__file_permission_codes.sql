-- 文件接口权限码（修订方案 §2.2 / P0-5）
--
-- 背景：/files/** 此前在 SecurityConfig 中不限方法 permitAll，任何人可匿名上传（落 tenant_id=0）。
-- 收紧后：读端点需登录，写端点（上传/删除）另需权限码 file:upload / file:delete。
--
-- 说明两点：
--   1) menu_id 置 NULL —— 文件上传是跨模块通用能力，没有专属菜单；
--      权限码加载走 sys_role_permission → sys_user_role（RoleMenuMapper.selectPermissionCodesByUser），不依赖 menu_id。
--   2) 授权范围给到 10159 租户下全部角色 —— 变更前"任何匿名者"都能上传，收紧为"登录用户 + 权限码"
--      已是严格收敛；按角色细分留待后续治理，避免本次变更误伤业务账号。
--
-- 取号：2026-09-22 已查两库 MAX(version) = 测试库 20260922.041 / 生产库 20260921.101，
--       取 20260922.201 留间隔（撞号会导致 jar 全环境无法启动）。

-- 1) 权限码
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (2026092220101, 10159, NULL, 'file:upload', '上传文件', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (2026092220102, 10159, NULL, 'file:delete', '删除文件', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW();

-- 2) 角色授权（三步链最后一环，缺了接口必 403）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202609222020000 + ROW_NUMBER() OVER (ORDER BY r.id, p.id),
  10159,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.tenant_id = 10159
  AND r.deleted = 0
  AND p.tenant_id = 10159
  AND p.code IN ('file:upload', 'file:delete')
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
