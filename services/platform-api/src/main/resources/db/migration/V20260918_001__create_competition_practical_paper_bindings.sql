-- Practical paper binding is a stage-scope association.
-- It intentionally does not depend on an execution plan, practical session, or scoring rule.

CREATE TABLE competition_practical_paper_binding (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_comp_practical_paper_binding_scope_paper (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, paper_snapshot_id, deleted
  ),
  KEY idx_comp_practical_paper_binding_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, deleted
  ),
  KEY idx_comp_practical_paper_binding_snapshot (
    tenant_id, paper_snapshot_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛事赛段工种等级与实操试卷快照绑定关系';
