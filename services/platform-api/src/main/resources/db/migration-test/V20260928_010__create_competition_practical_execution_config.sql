-- Stage-level running rules are stored separately from the reusable practical scoring snapshot.
CREATE TABLE competition_practical_execution_config (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_content_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  scoring_scheme_id BIGINT NOT NULL,
  exam_name VARCHAR(128) NOT NULL,
  duration_seconds BIGINT NOT NULL,
  start_mode VARCHAR(32) NOT NULL,
  allow_early_submit TINYINT NOT NULL DEFAULT 0,
  time_scoring_enabled TINYINT NOT NULL DEFAULT 0,
  overtime_rule_type VARCHAR(32) NOT NULL DEFAULT 'NONE',
  overtime_step_seconds BIGINT NULL,
  overtime_score_per_step DECIMAL(12,2) NULL,
  overtime_fixed_deduction DECIMAL(12,2) NULL,
  overtime_max_deduction DECIMAL(12,2) NULL,
  early_finish_rule_type VARCHAR(32) NOT NULL DEFAULT 'NONE',
  early_finish_step_seconds BIGINT NULL,
  early_finish_score_per_step DECIMAL(12,2) NULL,
  early_finish_fixed_bonus DECIMAL(12,2) NULL,
  early_finish_max_bonus DECIMAL(12,2) NULL,
  overtime_action VARCHAR(32) NOT NULL DEFAULT 'ALLOW_CONTINUE',
  max_duration_seconds BIGINT NULL,
  final_score_upper_limit DECIMAL(12,2) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_exec_start_mode
    CHECK (start_mode IN ('UNIFIED_START', 'MANUAL_START')),
  CONSTRAINT chk_comp_practical_exec_early_submit
    CHECK (allow_early_submit IN (0, 1)),
  CONSTRAINT chk_comp_practical_exec_time_score
    CHECK (time_scoring_enabled IN (0, 1)),
  CONSTRAINT chk_comp_practical_exec_overtime_rule
    CHECK (overtime_rule_type IN ('NONE', 'STEP_DEDUCTION', 'FIXED_DEDUCTION')),
  CONSTRAINT chk_comp_practical_exec_early_rule
    CHECK (early_finish_rule_type IN ('NONE', 'STEP_BONUS', 'FIXED_BONUS')),
  CONSTRAINT chk_comp_practical_exec_overtime_action
    CHECK (overtime_action IN ('ALLOW_CONTINUE', 'AUTO_FINISH')),
  CONSTRAINT chk_comp_practical_exec_duration
    CHECK (duration_seconds > 0 AND (overtime_action <> 'AUTO_FINISH'
      OR (max_duration_seconds IS NOT NULL AND max_duration_seconds >= duration_seconds))),
  CONSTRAINT chk_comp_practical_exec_overtime_values
    CHECK ((time_scoring_enabled = 0 OR overtime_rule_type <> 'STEP_DEDUCTION') OR
      (overtime_step_seconds IS NOT NULL AND overtime_step_seconds > 0
        AND overtime_score_per_step IS NOT NULL AND overtime_score_per_step >= 0
        AND overtime_max_deduction IS NOT NULL AND overtime_max_deduction >= 0)),
  CONSTRAINT chk_comp_practical_exec_fixed_deduction
    CHECK ((time_scoring_enabled = 0 OR overtime_rule_type <> 'FIXED_DEDUCTION') OR
      (overtime_fixed_deduction IS NOT NULL AND overtime_fixed_deduction >= 0)),
  CONSTRAINT chk_comp_practical_exec_early_values
    CHECK ((time_scoring_enabled = 0 OR early_finish_rule_type <> 'STEP_BONUS') OR
      (early_finish_step_seconds IS NOT NULL AND early_finish_step_seconds > 0
        AND early_finish_score_per_step IS NOT NULL AND early_finish_score_per_step >= 0
        AND early_finish_max_bonus IS NOT NULL AND early_finish_max_bonus >= 0)),
  CONSTRAINT chk_comp_practical_exec_fixed_bonus
    CHECK ((time_scoring_enabled = 0 OR early_finish_rule_type <> 'FIXED_BONUS') OR
      (early_finish_fixed_bonus IS NOT NULL AND early_finish_fixed_bonus >= 0)),
  CONSTRAINT chk_comp_practical_exec_score_limit
    CHECK (final_score_upper_limit > 0 AND version > 0),
  UNIQUE KEY uk_comp_practical_exec_config_stage (tenant_id, stage_id, deleted),
  KEY idx_comp_practical_exec_config_scheme (tenant_id, scoring_scheme_id, deleted),
  KEY idx_comp_practical_exec_config_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛段实操考试运行规则';

ALTER TABLE competition_practical_assignment
  ADD COLUMN standard_deadline_at DATETIME NULL COMMENT '标准考核截止时间（服务端开始时间+标准时长）' AFTER started_at,
  ADD COLUMN deadline_at DATETIME NULL COMMENT '自动结束硬截止时间（仅AUTO_FINISH配置有值）' AFTER standard_deadline_at,
  ADD COLUMN exam_ended_at DATETIME NULL COMMENT '考试实际结束时间（服务端记录）' AFTER deadline_at,
  ADD COLUMN paused_duration_seconds BIGINT NOT NULL DEFAULT 0 COMMENT '累计暂停时长秒数，供后续暂停恢复能力使用' AFTER exam_ended_at;
