-- P0：补齐企业调研 API 权限码 train:research_org:*，挂到「企业调研」菜单，并授予已有 pxyanjiu:view 的角色

SET @research_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'pxyan_jiu' AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT * FROM (
  SELECT 209100100000100001 AS id, 1 AS tenant_id, @research_menu_id AS menu_id,
         'train:research_org:view' AS code, '查看企业调研' AS name, 'view' AS action,
         'enabled' AS status, NOW() AS created_at, NOW() AS updated_at, 1 AS created_by, 1 AS updated_by, 0 AS deleted
  UNION ALL
  SELECT 209100100000100002, 1, @research_menu_id, 'train:research_org:create', '新建企业调研', 'create',
         'enabled', NOW(), NOW(), 1, 1, 0
  UNION ALL
  SELECT 209100100000100003, 1, @research_menu_id, 'train:research_org:update', '编辑企业调研', 'update',
         'enabled', NOW(), NOW(), 1, 1, 0
  UNION ALL
  SELECT 209100100000100004, 1, @research_menu_id, 'train:research_org:delete', '删除企业调研', 'delete',
         'enabled', NOW(), NOW(), 1, 1, 0
) AS seed
WHERE @research_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = seed.code AND p.deleted = 0
  );

-- 已有企业调研查看权限的角色，同步授予 research_org 全套
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  209100100000200000 + ROW_NUMBER() OVER (ORDER BY rp.tenant_id, rp.role_id, p_new.id) - 1,
  rp.tenant_id,
  rp.role_id,
  p_new.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_permission rp
INNER JOIN sys_permission p_old
  ON p_old.id = rp.permission_id
 AND p_old.tenant_id = rp.tenant_id
 AND p_old.code = 'pxyanjiu:view'
 AND p_old.deleted = 0
INNER JOIN sys_permission p_new
  ON p_new.tenant_id = rp.tenant_id
 AND p_new.code IN (
   'train:research_org:view',
   'train:research_org:create',
   'train:research_org:update',
   'train:research_org:delete'
 )
 AND p_new.deleted = 0
WHERE rp.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.tenant_id = rp.tenant_id
      AND x.role_id = rp.role_id
      AND x.permission_id = p_new.id
      AND x.deleted = 0
  );

-- 平台管理员兜底
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  209100100000300000 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id, p.id) - 1,
  r.tenant_id,
  r.id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
 AND p.code IN (
   'train:research_org:view',
   'train:research_org:create',
   'train:research_org:update',
   'train:research_org:delete'
 )
 AND p.deleted = 0
WHERE r.deleted = 0
  AND (r.id = 2000 OR UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN'))
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.tenant_id = r.tenant_id
      AND x.role_id = r.id
      AND x.permission_id = p.id
      AND x.deleted = 0
  );
