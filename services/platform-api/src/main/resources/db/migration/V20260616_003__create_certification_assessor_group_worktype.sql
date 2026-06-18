-- 考评组负责工种+等级范围关联表
-- 业务诉求：一个考评组可负责多个任务工种+等级，替代单值 work_type_name

CREATE TABLE IF NOT EXISTS certification_assessor_group_worktype (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  group_id BIGINT NOT NULL COMMENT '考评组id(关联 certification_task_assessor_group.id)',
  scope_id BIGINT NOT NULL COMMENT '任务工种范围id(关联 certification_task_scope.id)',
  work_type_name VARCHAR(120) NULL COMMENT '工种名(冗余便于展示)',
  level_code VARCHAR(40) NULL COMMENT '等级编码(冗余)',
  level_name VARCHAR(80) NULL COMMENT '等级名称(冗余)',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_scope (tenant_id, group_id, scope_id, deleted),
  KEY idx_group_scope_group (tenant_id, group_id, deleted),
  KEY idx_group_scope_scope (tenant_id, scope_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考评组负责工种+等级范围';

-- 数据迁移：旧单值 work_type_name -> 新关联行（仅匹配到 scope 的；不匹配保留旧列）
INSERT IGNORE INTO certification_assessor_group_worktype (
  id, tenant_id, group_id, scope_id, work_type_name, level_code, level_name, sort_order,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202606160030000000 + ROW_NUMBER() OVER (ORDER BY group_id, scope_id) AS id,
  tenant_id,
  group_id,
  scope_id,
  work_type_name,
  level_code,
  level_name,
  sort_order,
  NOW(), NOW(), 1, 1, 0
FROM (
  SELECT
    g.id AS group_id,
    g.tenant_id,
    s.id AS scope_id,
    s.work_type_name,
    s.level_code,
    s.level_name,
    s.sort_order
  FROM certification_task_assessor_group g
  INNER JOIN certification_task_scope s
    ON s.tenant_id = g.tenant_id
    AND s.task_id = g.task_id
    AND s.deleted = 0
    AND s.work_type_name = g.work_type_name
  WHERE g.deleted = 0
    AND g.work_type_name IS NOT NULL
) matched_scopes;
