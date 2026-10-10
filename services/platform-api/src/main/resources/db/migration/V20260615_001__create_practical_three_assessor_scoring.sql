ALTER TABLE certification_task_scope
  ADD COLUMN work_type_id BIGINT NULL COMMENT '标准工种id' AFTER certification_type;

ALTER TABLE exam_operate_detail
  ADD COLUMN scoring_template_json JSON NULL COMMENT '评分要素模板快照源' AFTER exam_content;

ALTER TABLE exam_operate_paper_detail
  ADD COLUMN scoring_template_json JSON NULL COMMENT '评分要素模板快照' AFTER exam_content;

UPDATE exam_operate_paper_detail
SET weight = 100
WHERE weight IS NULL OR weight <> 100;

ALTER TABLE exam_operate_paper_detail
  MODIFY weight INT NOT NULL DEFAULT 100 COMMENT '兼容字段，实操评分固定100%且不参与计算';

ALTER TABLE certification_assessor_task
  ADD COLUMN candidate_id BIGINT NULL COMMENT '考生id' AFTER leader_assessor_id,
  ADD COLUMN paper_id BIGINT NULL COMMENT '实操试卷id' AFTER candidate_id,
  ADD COLUMN paper_question_id BIGINT NULL COMMENT '实操试题id' AFTER paper_id,
  ADD COLUMN revision_no INT NOT NULL DEFAULT 1 COMMENT '重评修订号' AFTER paper_question_id,
  ADD COLUMN version_no INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER revision_no,
  ADD COLUMN candidate_snapshot_json JSON NULL COMMENT '考生快照' AFTER version_no,
  ADD COLUMN question_snapshot_json JSON NULL COMMENT '题目与评分模板快照' AFTER candidate_snapshot_json,
  ADD COLUMN returned_at DATETIME NULL COMMENT '退回时间' AFTER submitted_at,
  ADD COLUMN returned_by BIGINT NULL COMMENT '退回人' AFTER returned_at,
  ADD COLUMN return_reason VARCHAR(1000) NULL COMMENT '退回原因' AFTER returned_by,
  ADD UNIQUE KEY uk_cert_assessor_scoring_task (
    tenant_id, task_id, candidate_id, paper_question_id, assessor_id, deleted
  ),
  ADD KEY idx_cert_assessor_scoring_candidate (
    tenant_id, task_id, candidate_id, paper_question_id, status, deleted
  );

ALTER TABLE certification_assessor_task_score
  ADD COLUMN criterion VARCHAR(1000) NULL COMMENT '评分标准快照' AFTER step_name,
  ADD COLUMN max_deduction DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '最大扣分' AFTER max_score,
  ADD COLUMN deduction_step DECIMAL(8,2) NOT NULL DEFAULT 1.00 COMMENT '扣分步长' AFTER max_deduction,
  ADD COLUMN deducted_score DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '实际扣分' AFTER deduction_step,
  ADD COLUMN awarded_score DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '要素得分' AFTER deducted_score,
  ADD UNIQUE KEY uk_cert_assessor_score_item (tenant_id, assessor_task_id, step_no, deleted);

CREATE TABLE IF NOT EXISTS certification_practical_scoring_config (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  scope_id BIGINT NOT NULL COMMENT '任务工种等级范围id',
  work_type_id BIGINT NOT NULL COMMENT '标准工种id',
  work_type_name VARCHAR(120) NOT NULL COMMENT '工种名称快照',
  level_code VARCHAR(40) NOT NULL COMMENT '等级编码',
  level_name VARCHAR(80) NULL COMMENT '等级名称快照',
  exam_date DATE NOT NULL COMMENT '考试日期',
  paper_id BIGINT NOT NULL COMMENT '当天唯一实操试卷id',
  group_id BIGINT NOT NULL COMMENT '当天唯一三人考评组id',
  status VARCHAR(40) NOT NULL DEFAULT 'configured',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_config (
    tenant_id, task_id, work_type_id, level_code, exam_date, deleted
  ),
  KEY idx_cert_practical_config_task (tenant_id, task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实操评分任务配置';

CREATE TABLE IF NOT EXISTS certification_practical_question_result (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  paper_question_id BIGINT NOT NULL,
  submitted_count INT NOT NULL DEFAULT 0,
  average_score DECIMAL(10,4) NULL COMMENT '三人平均原始分',
  final_score INT NULL COMMENT 'HALF_UP取整后的试题最终分',
  status VARCHAR(40) NOT NULL DEFAULT 'pending',
  calculated_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_question_result (
    tenant_id, task_id, candidate_id, paper_question_id, deleted
  ),
  KEY idx_cert_practical_question_result_candidate (
    tenant_id, task_id, candidate_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考生实操试题最终成绩';

CREATE TABLE IF NOT EXISTS certification_practical_candidate_result (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  question_count INT NOT NULL DEFAULT 0,
  completed_question_count INT NOT NULL DEFAULT 0,
  total_score INT NULL COMMENT '所有试题最终整数分之和',
  status VARCHAR(40) NOT NULL DEFAULT 'pending',
  calculated_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_candidate_result (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_practical_candidate_result_task (tenant_id, task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考生实操总成绩';

CREATE TABLE IF NOT EXISTS certification_assessor_task_history (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  assessor_task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  paper_question_id BIGINT NOT NULL,
  assessor_id BIGINT NOT NULL,
  revision_no INT NOT NULL,
  action VARCHAR(40) NOT NULL COMMENT 'draft/submit/return',
  version_no INT NOT NULL,
  total_score DECIMAL(8,2) NULL,
  snapshot_json JSON NOT NULL,
  reason VARCHAR(1000) NULL,
  operated_at DATETIME NOT NULL,
  operated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_assessor_task_history (tenant_id, assessor_task_id, operated_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考评员评分提交与退回历史';

CREATE TABLE IF NOT EXISTS certification_practical_start_session (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  config_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  candidate_confirmed_at DATETIME NULL,
  candidate_confirmed_by BIGINT NULL,
  assessor_confirmations_json JSON NULL COMMENT '已确认考评员ID及确认时间',
  status VARCHAR(40) NOT NULL DEFAULT 'waiting' COMMENT 'waiting/ready/started',
  version_no INT NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  started_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_start_session (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_practical_start_status (tenant_id, task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实操考生与三名考评员统一开考会话';
