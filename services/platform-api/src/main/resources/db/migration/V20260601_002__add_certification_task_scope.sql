CREATE TABLE IF NOT EXISTS certification_task_scope (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  work_type_name VARCHAR(120) NULL COMMENT '工种',
  level_code VARCHAR(40) NULL COMMENT '等级编码',
  level_name VARCHAR(80) NULL COMMENT '等级名称',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_scope_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定任务工种等级范围';

-- 迁移旧字段（每个任务生成一条 scope）
INSERT IGNORE INTO certification_task_scope (
  id, tenant_id, task_id, work_type_name, level_code, level_name, sort_order, remark,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  t.id AS id,
  t.tenant_id,
  t.id AS task_id,
  t.work_type_name,
  t.level_code,
  t.level_name,
  1 AS sort_order,
  NULL AS remark,
  NOW(), NOW(), 1, 1, 0
FROM certification_task t
WHERE t.deleted = 0
  AND (t.work_type_name IS NOT NULL OR t.level_code IS NOT NULL OR t.level_name IS NOT NULL);
