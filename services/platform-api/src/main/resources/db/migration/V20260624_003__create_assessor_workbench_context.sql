CREATE TABLE IF NOT EXISTS certification_assessor_workbench_context (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL COMMENT '租户id',
  leader_assessor_id BIGINT NOT NULL COMMENT '组长考评员id',
  active_task_id BIGINT NOT NULL COMMENT '组长当前指定的认定任务id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_assessor_workbench_context (tenant_id, leader_assessor_id, deleted),
  KEY idx_cert_assessor_workbench_active_task (tenant_id, active_task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考评平板当前认定任务上下文';
