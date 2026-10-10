-- COMP-SCORE-02: versioned score facts, practical-score traceability and immutable snapshots.
-- Version 20260727.006 was selected after querying the actual flyway_schema_history,
-- whose maximum successful version was 20260727.005 on 2026-07-30.

CREATE TABLE competition_score_source (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL COMMENT 'theory/simulation/practical',
  raw_score DECIMAL(12,6) NOT NULL,
  confirmation_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  source_system VARCHAR(64) NOT NULL DEFAULT 'competition',
  source_reference VARCHAR(255) NULL COMMENT '考试会话、仿真流水或实操汇总引用',
  score_rule_id BIGINT NOT NULL COMMENT 'competition_score_rule.id',
  revision_no INT NOT NULL,
  current_flag TINYINT NOT NULL DEFAULT 1 COMMENT '1当前版本，0历史版本',
  active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND current_flag = 1 THEN 1 ELSE NULL END
  ) STORED,
  achieved_at DATETIME NOT NULL,
  confirmed_at DATETIME NULL,
  confirmed_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_score_source_type CHECK (
    source_type IN ('theory', 'simulation', 'practical')
  ),
  CONSTRAINT chk_comp_score_source_score CHECK (raw_score >= 0 AND raw_score <= 100),
  CONSTRAINT chk_comp_score_source_status CHECK (
    confirmation_status IN ('pending', 'confirmed', 'voided')
  ),
  CONSTRAINT chk_comp_score_source_revision CHECK (revision_no > 0),
  CONSTRAINT chk_comp_score_source_current CHECK (current_flag IN (0, 1)),
  CONSTRAINT chk_comp_score_source_confirmed CHECK (
    confirmation_status <> 'confirmed'
    OR (confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL)
  ),
  UNIQUE KEY uk_comp_score_source_revision (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, source_type, revision_no, deleted
  ),
  UNIQUE KEY uk_comp_score_source_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, source_type, active_current_key
  ),
  KEY idx_comp_score_source_rule (
    tenant_id, score_rule_id, confirmation_status, deleted
  ),
  KEY idx_comp_score_source_reference (
    tenant_id, source_system, source_reference, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论、仿真、实操成绩来源修订';

CREATE TABLE competition_referee_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL COMMENT 'competition_event_member.id',
  assignment_role VARCHAR(32) NOT NULL COMMENT 'official/standby',
  contestant_member_id BIGINT NOT NULL DEFAULT 0 COMMENT '0表示当前工种全部选手',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  active_assignment_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'active' THEN 1 ELSE NULL END
  ) STORED,
  replaced_assignment_id BIGINT NULL COMMENT '备用裁判补位记录指向被替换分配',
  replacement_reason VARCHAR(500) NULL,
  assigned_at DATETIME NOT NULL,
  ended_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_referee_assignment_role CHECK (
    assignment_role IN ('official', 'standby')
  ),
  CONSTRAINT chk_comp_referee_assignment_status CHECK (
    status IN ('active', 'replaced', 'withdrawn')
  ),
  CONSTRAINT chk_comp_referee_assignment_scope CHECK (contestant_member_id >= 0),
  CONSTRAINT chk_comp_referee_assignment_replaced_status CHECK (
    status <> 'replaced' OR ended_at IS NOT NULL
  ),
  CONSTRAINT chk_comp_referee_assignment_replacement_trace CHECK (
    replaced_assignment_id IS NULL OR replacement_reason IS NOT NULL
  ),
  UNIQUE KEY uk_comp_referee_assignment_active (
    tenant_id, event_id, stage_id, event_work_type_id,
    referee_member_id, contestant_member_id, active_assignment_key
  ),
  KEY idx_comp_referee_assignment_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, assignment_role, status, deleted
  ),
  KEY idx_comp_referee_assignment_replaced (
    tenant_id, replaced_assignment_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛正式及现场备用裁判分配';

CREATE TABLE competition_referee_avoidance (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  relation_type VARCHAR(32) NOT NULL COMMENT 'mentor/relative/same_org/other',
  reason VARCHAR(500) NOT NULL,
  configured_by BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  active_avoidance_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'active' THEN 1 ELSE NULL END
  ) STORED,
  configured_at DATETIME NOT NULL,
  ended_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_referee_avoidance_type CHECK (
    relation_type IN ('mentor', 'relative', 'same_org', 'other')
  ),
  CONSTRAINT chk_comp_referee_avoidance_status CHECK (
    status IN ('active', 'inactive')
  ),
  CONSTRAINT chk_comp_referee_avoidance_ended CHECK (
    status <> 'inactive' OR ended_at IS NOT NULL
  ),
  UNIQUE KEY uk_comp_referee_avoidance_active (
    tenant_id, event_id, stage_id, event_work_type_id,
    referee_member_id, contestant_member_id, active_avoidance_key
  ),
  KEY idx_comp_referee_avoidance_contestant (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛裁判与选手禁评关系';

CREATE TABLE competition_practical_score (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL,
  referee_assignment_id BIGINT NOT NULL,
  criteria_version_reference VARCHAR(128) NOT NULL COMMENT '已锁定评分细则版本引用',
  revision_no INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'assigned',
  total_score DECIMAL(12,6) NULL,
  current_flag TINYINT NOT NULL DEFAULT 1 COMMENT '1当前版本，0历史版本',
  active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND current_flag = 1 THEN 1 ELSE NULL END
  ) STORED,
  source_system VARCHAR(64) NOT NULL DEFAULT 'competition'
    COMMENT '为后续认定评分适配保留来源边界',
  source_reference VARCHAR(255) NULL,
  replaced_score_id BIGINT NULL COMMENT '备用裁判评分指向被替换评分',
  replacement_reason VARCHAR(500) NULL,
  submitted_at DATETIME NULL,
  locked_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_score_revision CHECK (revision_no > 0),
  CONSTRAINT chk_comp_practical_score_status CHECK (
    status IN (
      'assigned', 'draft', 'submitted_locked',
      'unlocked_for_rescore', 'recused', 'voided'
    )
  ),
  CONSTRAINT chk_comp_practical_score_value CHECK (
    total_score IS NULL OR (total_score >= 0 AND total_score <= 100)
  ),
  CONSTRAINT chk_comp_practical_score_current CHECK (current_flag IN (0, 1)),
  CONSTRAINT chk_comp_practical_score_locked CHECK (
    status <> 'submitted_locked'
    OR (total_score IS NOT NULL AND submitted_at IS NOT NULL AND locked_at IS NOT NULL)
  ),
  CONSTRAINT chk_comp_practical_score_replacement CHECK (
    replaced_score_id IS NULL OR replacement_reason IS NOT NULL
  ),
  UNIQUE KEY uk_comp_practical_score_revision (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, referee_member_id,
    criteria_version_reference, revision_no, deleted
  ),
  UNIQUE KEY uk_comp_practical_score_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, referee_member_id,
    criteria_version_reference, active_current_key
  ),
  KEY idx_comp_practical_score_average (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, status, current_flag, deleted
  ),
  KEY idx_comp_practical_score_assignment (
    tenant_id, referee_assignment_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操裁判评分修订';

CREATE TABLE competition_score_unlock_request (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  practical_score_id BIGINT NOT NULL,
  request_no INT NOT NULL,
  request_reason VARCHAR(1000) NOT NULL,
  evidence_file_id BIGINT NULL,
  applicant_member_id BIGINT NOT NULL COMMENT '主裁判长赛事成员ID',
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  active_pending_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'pending' THEN 1 ELSE NULL END
  ) STORED,
  decided_by BIGINT NULL COMMENT '系统管理员用户ID',
  decision_comment VARCHAR(1000) NULL,
  requested_at DATETIME NOT NULL,
  decided_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_unlock_request_no CHECK (request_no > 0),
  CONSTRAINT chk_comp_unlock_request_status CHECK (
    status IN ('pending', 'approved', 'rejected')
  ),
  CONSTRAINT chk_comp_unlock_request_decision CHECK (
    status = 'pending' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL)
  ),
  UNIQUE KEY uk_comp_unlock_request_version (
    tenant_id, practical_score_id, request_no, deleted
  ),
  UNIQUE KEY uk_comp_unlock_request_pending (
    tenant_id, practical_score_id, active_pending_key
  ),
  KEY idx_comp_unlock_request_status (
    tenant_id, status, requested_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操评分解锁申请与审批历史';

CREATE TABLE competition_total_score_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  score_rule_id BIGINT NOT NULL,
  theory_source_id BIGINT NOT NULL,
  simulation_source_id BIGINT NOT NULL,
  practical_source_id BIGINT NOT NULL,
  theory_score DECIMAL(12,6) NOT NULL,
  simulation_score DECIMAL(12,6) NOT NULL,
  practical_score DECIMAL(12,6) NOT NULL,
  simulation_practical_score DECIMAL(12,6) NOT NULL,
  theory_weight DECIMAL(5,2) NOT NULL,
  simulation_practical_weight DECIMAL(5,2) NOT NULL,
  total_score DECIMAL(12,6) NULL,
  calculation_version INT NOT NULL,
  confirmation_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  current_flag TINYINT NOT NULL DEFAULT 1 COMMENT '1当前版本，0历史版本',
  active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND current_flag = 1 THEN 1 ELSE NULL END
  ) STORED,
  calculated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_total_score_inputs CHECK (
    theory_score BETWEEN 0 AND 100
    AND simulation_score BETWEEN 0 AND 100
    AND practical_score BETWEEN 0 AND 100
    AND simulation_practical_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_total_score_weights CHECK (
    theory_weight BETWEEN 0 AND 100
    AND simulation_practical_weight BETWEEN 0 AND 100
    AND theory_weight + simulation_practical_weight = 100
  ),
  CONSTRAINT chk_comp_total_score_value CHECK (
    total_score IS NULL OR total_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_total_score_version CHECK (calculation_version > 0),
  CONSTRAINT chk_comp_total_score_status CHECK (
    confirmation_status IN ('pending', 'confirmed', 'voided')
  ),
  CONSTRAINT chk_comp_total_score_confirmed CHECK (
    confirmation_status <> 'confirmed' OR total_score IS NOT NULL
  ),
  CONSTRAINT chk_comp_total_score_current CHECK (current_flag IN (0, 1)),
  UNIQUE KEY uk_comp_total_score_version (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, score_rule_id, calculation_version, deleted
  ),
  UNIQUE KEY uk_comp_total_score_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_member_id, score_rule_id, active_current_key
  ),
  KEY idx_comp_total_score_sources (
    tenant_id, theory_source_id, simulation_source_id, practical_source_id, deleted
  ),
  KEY idx_comp_total_score_ranking (
    tenant_id, event_id, stage_id, event_work_type_id,
    score_rule_id, confirmation_status, total_score, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛制B总分计算不可变快照';

CREATE TABLE competition_ranking_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  score_rule_id BIGINT NOT NULL,
  snapshot_version INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  generation_reason VARCHAR(500) NULL,
  generated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_ranking_snapshot_version CHECK (snapshot_version > 0),
  CONSTRAINT chk_comp_ranking_snapshot_status CHECK (
    status IN ('draft', 'confirmed', 'published')
  ),
  UNIQUE KEY uk_comp_ranking_snapshot_version (
    tenant_id, event_id, stage_id, event_work_type_id,
    score_rule_id, snapshot_version, deleted
  ),
  KEY idx_comp_ranking_snapshot_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    score_rule_id, status, generated_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛独立工种排名快照';

CREATE TABLE competition_ranking_entry (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  ranking_snapshot_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  total_score_snapshot_id BIGINT NULL COMMENT '缺考条目可为空',
  total_score DECIMAL(12,6) NOT NULL DEFAULT 0,
  display_rank INT NULL COMMENT '缺考选手为空',
  tie_key VARCHAR(255) NULL COMMENT '应用层已冻结完整排序键的并列摘要',
  tied_flag TINYINT NOT NULL DEFAULT 0,
  contestant_status VARCHAR(32) NOT NULL,
  sort_order INT NOT NULL,
  active_entry_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_ranking_entry_score CHECK (total_score BETWEEN 0 AND 100),
  CONSTRAINT chk_comp_ranking_entry_rank CHECK (display_rank IS NULL OR display_rank > 0),
  CONSTRAINT chk_comp_ranking_entry_tied CHECK (tied_flag IN (0, 1)),
  CONSTRAINT chk_comp_ranking_entry_status CHECK (
    contestant_status IN ('completed', 'absent')
  ),
  CONSTRAINT chk_comp_ranking_entry_absent CHECK (
    (contestant_status = 'absent' AND display_rank IS NULL AND total_score = 0)
    OR (
      contestant_status = 'completed'
      AND display_rank IS NOT NULL
      AND total_score_snapshot_id IS NOT NULL
      AND tie_key IS NOT NULL
    )
  ),
  CONSTRAINT chk_comp_ranking_entry_order CHECK (sort_order > 0),
  UNIQUE KEY uk_comp_ranking_entry_contestant (
    tenant_id, ranking_snapshot_id, contestant_member_id, active_entry_key
  ),
  UNIQUE KEY uk_comp_ranking_entry_order (
    tenant_id, ranking_snapshot_id, sort_order, active_entry_key
  ),
  KEY idx_comp_ranking_entry_rank (
    tenant_id, ranking_snapshot_id, display_rank, sort_order, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛排名快照条目';
