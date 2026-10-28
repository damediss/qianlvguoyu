-- 企业功能配置：菜单白名单 + 可授角色包（后写覆盖，一企一行）

CREATE TABLE IF NOT EXISTS train_enterprise_feature_config (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业组织 ID（sys_org.company）',
  configurer_org_id BIGINT NOT NULL DEFAULT 0 COMMENT '配置方组织ID，超管为0',
  menu_codes_json TEXT NOT NULL COMMENT '允许菜单 code 白名单 JSON 数组',
  role_ids_json TEXT NOT NULL COMMENT '可授角色 ID 白名单 JSON 数组',
  constrain_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用约束；0=关闭（等同未配置）',
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tefc_tenant_enterprise (tenant_id, enterprise_id, deleted),
  KEY idx_tefc_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业功能配置';

-- 权限码：功能配置
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027005000100000 + ROW_NUMBER() OVER (ORDER BY m.tenant_id) - 1,
  m.tenant_id,
  m.id,
  'train:company:feature',
  '企业功能配置',
  'update',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.code = 'train_company'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = m.tenant_id AND p.code = 'train:company:feature' AND p.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027005000200000 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id, p.id) - 1,
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
 AND p.code = 'train:company:feature'
 AND p.deleted = 0
WHERE r.deleted = 0
  AND (
    r.id = 2000
    OR UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN')
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

-- 有企业管理菜单的角色同步授予 feature
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027005000300000 + ROW_NUMBER() OVER (ORDER BY rm.tenant_id, rm.role_id, p.id) - 1,
  rm.tenant_id,
  rm.role_id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id
  AND p.code = 'train:company:feature'
  AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_company'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id
      AND rp.role_id = rm.role_id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
