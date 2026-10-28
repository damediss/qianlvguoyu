-- M1：机构–企业业务关联（培训需求选中 → 永久可查看，不改组织树）

CREATE TABLE IF NOT EXISTS train_org_enterprise_link (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  org_id BIGINT NOT NULL COMMENT '培训机构组织ID',
  enterprise_id BIGINT NOT NULL COMMENT '企业组织ID（sys_org.company）',
  source VARCHAR(32) NOT NULL DEFAULT 'demand' COMMENT '来源：demand 等',
  source_id BIGINT NULL COMMENT '来源单据ID',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/inactive（本期不自动失效）',
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_toel_tenant_org_enterprise (tenant_id, org_id, enterprise_id, deleted),
  KEY idx_toel_org (tenant_id, org_id, status, deleted),
  KEY idx_toel_enterprise (tenant_id, enterprise_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构-企业业务关联';

-- 历史需求回填：创建人所属机构 → 需求目标企业（排除企业/大师账号创建人）
INSERT INTO train_org_enterprise_link (
  id, tenant_id, org_id, enterprise_id, source, source_id, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210028001000000000 + ROW_NUMBER() OVER (ORDER BY d.id) - 1,
  d.tenant_id,
  u.org_id,
  d.org_id,
  'demand',
  d.id,
  'active',
  NOW(),
  NOW(),
  d.created_by,
  d.created_by,
  0
FROM demand_train d
INNER JOIN sys_user u
  ON u.id = d.created_by
 AND u.deleted = 0
INNER JOIN sys_org creator_org
  ON creator_org.id = u.org_id
 AND creator_org.deleted = 0
 AND creator_org.org_type NOT IN ('company', 'master')
INNER JOIN sys_org enterprise
  ON enterprise.id = d.org_id
 AND enterprise.deleted = 0
 AND enterprise.org_type = 'company'
WHERE d.deleted = 0
  AND d.org_id IS NOT NULL
  AND d.org_id <> 0
  AND u.org_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM train_org_enterprise_link l
    WHERE l.tenant_id = d.tenant_id
      AND l.org_id = u.org_id
      AND l.enterprise_id = d.org_id
      AND l.deleted = 0
  );
