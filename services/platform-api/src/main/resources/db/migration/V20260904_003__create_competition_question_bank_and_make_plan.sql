-- 竞赛独立题库与组卷计划。
-- 竞赛题库不得复用课程题目（exam_exam_detail / exam_operate_detail）或课程组卷表。

CREATE TABLE competition_question (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL COMMENT 'theory-理论, practical-实操',
  question_type VARCHAR(32) NOT NULL COMMENT 'single/multi/judge/fill/practical',
  sort_order INT NOT NULL COMMENT '题目序号',
  stem LONGTEXT NOT NULL COMMENT '题干',
  score DECIMAL(12,2) NOT NULL,
  reference_answer TEXT NULL,
  fill_answer TEXT NULL,
  options_json JSON NULL COMMENT 'A-H选项对象',
  difficulty INT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  source_batch_id BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_question_content_type CHECK (content_type IN ('theory', 'practical')),
  CONSTRAINT chk_competition_question_type CHECK (
    (content_type = 'theory' AND question_type IN ('single', 'multi', 'judge', 'fill'))
    OR (content_type = 'practical' AND question_type = 'practical')
  ),
  CONSTRAINT chk_competition_question_order CHECK (sort_order > 0),
  CONSTRAINT chk_competition_question_score CHECK (score > 0),
  CONSTRAINT chk_competition_question_difficulty CHECK (difficulty IS NULL OR difficulty BETWEEN 1 AND 5),
  CONSTRAINT chk_competition_question_status CHECK (status IN ('enabled', 'disabled')),
  UNIQUE KEY uk_competition_question_scope_order (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, question_type, sort_order, deleted
  ),
  KEY idx_competition_question_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, question_type, difficulty, status, deleted
  ),
  KEY idx_competition_question_batch (tenant_id, source_batch_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛独立题库公共题目';

CREATE TABLE competition_practical_question_detail (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  operation_content LONGTEXT NULL COMMENT '操作内容',
  student_prepare TEXT NULL COMMENT '考生准备',
  equipment_prepare TEXT NULL COMMENT '设备准备',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_practical_question_detail (tenant_id, question_id, deleted),
  KEY idx_competition_practical_question_detail_question (tenant_id, question_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操题专属详情';

CREATE TABLE competition_practical_question_score_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  item_no INT NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  criterion TEXT NULL COMMENT '评分标准',
  max_score DECIMAL(12,2) NOT NULL,
  max_deduction DECIMAL(12,2) NOT NULL,
  deduction_step DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_practical_item_no CHECK (item_no > 0),
  CONSTRAINT chk_competition_practical_item_scores CHECK (
    max_score > 0 AND max_deduction >= 0 AND deduction_step > 0 AND max_deduction <= max_score
  ),
  CONSTRAINT chk_competition_practical_item_status CHECK (status IN ('enabled', 'disabled')),
  UNIQUE KEY uk_competition_practical_question_item (tenant_id, question_id, item_no, deleted),
  KEY idx_competition_practical_question_items (tenant_id, question_id, status, item_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操题评分项模板';

CREATE TABLE competition_question_import_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  temp_file_path VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  total_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failure_count INT NOT NULL DEFAULT 0,
  error_detail JSON NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_question_batch_content_type CHECK (content_type IN ('theory', 'practical')),
  CONSTRAINT chk_competition_question_batch_status CHECK (
    status IN ('pending', 'processing', 'succeeded', 'failed')
  ),
  CONSTRAINT chk_competition_question_batch_counts CHECK (
    total_count >= 0 AND success_count >= 0 AND failure_count >= 0
  ),
  KEY idx_competition_question_batch_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, created_at, deleted
  ),
  UNIQUE KEY uk_competition_question_batch_id (tenant_id, id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛试题导入批次与进度';

CREATE TABLE competition_make_exam_plan (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL,
  title VARCHAR(255) NOT NULL,
  exam_type VARCHAR(32) NULL,
  duration_minutes INT NOT NULL,
  total_score DECIMAL(12,2) NOT NULL,
  pass_score DECIMAL(12,2) NOT NULL,
  paper_count INT NOT NULL DEFAULT 2,
  plan_version INT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  comments VARCHAR(2000) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_make_plan_type CHECK (content_type IN ('theory', 'practical')),
  CONSTRAINT chk_competition_make_plan_duration CHECK (duration_minutes > 0),
  CONSTRAINT chk_competition_make_plan_score CHECK (
    total_score > 0 AND pass_score >= 0 AND pass_score <= total_score
  ),
  CONSTRAINT chk_competition_make_plan_papers CHECK (
    (content_type = 'theory' AND paper_count = 2) OR (content_type = 'practical' AND paper_count = 1)
  ),
  CONSTRAINT chk_competition_make_plan_status CHECK (status IN ('draft', 'published', 'withdrawn')),
  UNIQUE KEY uk_competition_make_plan_version (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, plan_version, deleted
  ),
  KEY idx_competition_make_plan_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛组卷计划';

CREATE TABLE competition_make_exam_plan_detail (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  question_count INT NOT NULL,
  point DECIMAL(12,2) NOT NULL,
  difficulty_start INT NULL,
  difficulty_end INT NULL,
  sort_order INT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_make_plan_detail_count CHECK (question_count > 0),
  CONSTRAINT chk_competition_make_plan_detail_point CHECK (point > 0),
  CONSTRAINT chk_competition_make_plan_detail_difficulty CHECK (
    (difficulty_start IS NULL OR difficulty_start BETWEEN 1 AND 5)
    AND (difficulty_end IS NULL OR difficulty_end BETWEEN 1 AND 5)
    AND (difficulty_start IS NULL OR difficulty_end IS NULL OR difficulty_start <= difficulty_end)
  ),
  UNIQUE KEY uk_competition_make_plan_detail_order (tenant_id, plan_id, sort_order, deleted),
  KEY idx_competition_make_plan_detail_plan (tenant_id, plan_id, question_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛组卷计划题型明细';

CREATE TABLE competition_make_exam_paper_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL,
  paper_code VARCHAR(16) NOT NULL,
  version_no INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  duration_minutes INT NOT NULL,
  question_count INT NOT NULL,
  total_score DECIMAL(12,2) NOT NULL,
  pass_score DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  random_seed VARCHAR(64) NULL,
  source_hash CHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_make_snapshot_type CHECK (content_type IN ('theory', 'practical')),
  CONSTRAINT chk_competition_make_snapshot_code CHECK (
    (content_type = 'theory' AND paper_code IN ('A', 'B'))
    OR (content_type = 'practical' AND paper_code = 'P')
  ),
  CONSTRAINT chk_competition_make_snapshot_status CHECK (status IN ('draft', 'published', 'deprecated')),
  CONSTRAINT chk_competition_make_snapshot_score CHECK (
    duration_minutes > 0 AND question_count > 0 AND total_score > 0
    AND pass_score >= 0 AND pass_score <= total_score
  ),
  UNIQUE KEY uk_competition_make_snapshot_version (
    tenant_id, plan_id, paper_code, version_no, deleted
  ),
  KEY idx_competition_make_snapshot_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛组卷计划不可变试卷快照';

CREATE TABLE competition_make_exam_paper_question (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  source_question_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  stem LONGTEXT NOT NULL,
  options_json JSON NULL,
  reference_answer TEXT NULL,
  fill_answer TEXT NULL,
  score DECIMAL(12,2) NOT NULL,
  difficulty INT NULL,
  operation_content LONGTEXT NULL,
  student_prepare TEXT NULL,
  equipment_prepare TEXT NULL,
  scoring_items_json JSON NULL COMMENT '实操评分项深复制快照',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_make_snapshot_question_order CHECK (sort_order > 0),
  CONSTRAINT chk_competition_make_snapshot_question_score CHECK (score > 0),
  UNIQUE KEY uk_competition_make_snapshot_question_order (
    tenant_id, paper_snapshot_id, sort_order, deleted
  ),
  UNIQUE KEY uk_competition_make_snapshot_question_source (
    tenant_id, paper_snapshot_id, source_question_id, deleted
  ),
  KEY idx_competition_make_snapshot_question_paper (
    tenant_id, paper_snapshot_id, question_type, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛试卷快照题目深复制';

-- 现有赛段执行表继续兼容历史课程来源；新竞赛计划通过该字段绑定独立快照。
ALTER TABLE competition_theory_paper_snapshot
  MODIFY COLUMN source_course_id BIGINT NULL,
  MODIFY COLUMN source_plan_id BIGINT NULL,
  MODIFY COLUMN source_paper_id BIGINT NULL,
  ADD COLUMN source_competition_paper_snapshot_id BIGINT NULL AFTER source_paper_id,
  ADD KEY idx_competition_theory_snapshot_competition_source
    (tenant_id, source_competition_paper_snapshot_id, deleted);

ALTER TABLE competition_theory_paper_question
  ADD COLUMN fill_answer LONGTEXT NULL AFTER correct_answers_json,
  DROP CHECK chk_comp_theory_question_type,
  ADD CONSTRAINT chk_comp_theory_question_type CHECK (
    question_type IN ('single', 'multi', 'judge', 'fill')
  );

ALTER TABLE competition_practical_session
  ADD COLUMN paper_snapshot_id BIGINT NULL AFTER resource_id,
  ADD KEY idx_competition_practical_session_paper_snapshot
    (tenant_id, paper_snapshot_id, deleted);

ALTER TABLE competition_practical_score_entry
  MODIFY COLUMN rule_item_id BIGINT NULL,
  ADD COLUMN paper_question_id BIGINT NULL AFTER rule_item_id,
  ADD COLUMN paper_item_no INT NULL AFTER paper_question_id,
  ADD CONSTRAINT chk_comp_practical_score_entry_snapshot_key CHECK (
    (paper_question_id IS NULL AND paper_item_no IS NULL)
    OR (paper_question_id IS NOT NULL AND paper_item_no IS NOT NULL AND paper_item_no > 0)
  ),
  ADD UNIQUE KEY uk_comp_practical_score_entry_paper_item (
    tenant_id, score_sheet_id, paper_question_id, paper_item_no, deleted
  );

-- 竞赛题库与组卷计划菜单及权限。使用业务编码查找父菜单，避免依赖环境中的雪花 ID。
SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_foundation' AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609040301, 1, @competition_root_menu_id, 'directory', 'competition_question_bank', '竞赛试题管理',
 '/competition/question-bank', NULL, 'competition:question:view', 'FormOutlined', 30, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040302, 1, 202609040301, 'page', 'competition_theory_question', '理论试题',
 '/competition/question-bank/theory', 'competition/CompetitionQuestionBank', 'competition:question:view',
 'ProfileOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040303, 1, 202609040301, 'page', 'competition_practical_question', '实操试题',
 '/competition/question-bank/practical', 'competition/CompetitionQuestionBank', 'competition:question:view',
 'ToolOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040304, 1, @competition_root_menu_id, 'page', 'competition_make_exam_plan', '竞赛组卷计划',
 '/competition/make-exam-plans', 'competition/CompetitionMakeExamPlan', 'competition:make-plan:view',
 'FileTextOutlined', 31, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609040401, 1, 202609040301, 'competition:question:view', '查看竞赛试题', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040402, 1, 202609040301, 'competition:question:create', '新增竞赛试题', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040403, 1, 202609040301, 'competition:question:update', '修改竞赛试题', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040404, 1, 202609040301, 'competition:question:delete', '删除竞赛试题', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040405, 1, 202609040301, 'competition:question:import', '导入竞赛试题', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040406, 1, 202609040304, 'competition:make-plan:view', '查看竞赛组卷计划', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040407, 1, 202609040304, 'competition:make-plan:create', '创建竞赛组卷计划', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040408, 1, 202609040304, 'competition:make-plan:update', '修改竞赛组卷计划', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040409, 1, 202609040304, 'competition:make-plan:delete', '删除竞赛组卷计划', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
(202609040410, 1, 202609040304, 'competition:make-plan:publish', '发布竞赛组卷计划', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609040501, 1, 2000, 202609040301, NOW(), NOW(), 1, 1, 0),
(202609040502, 1, 2000, 202609040302, NOW(), NOW(), 1, 1, 0),
(202609040503, 1, 2000, 202609040303, NOW(), NOW(), 1, 1, 0),
(202609040504, 1, 2000, 202609040304, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609040601, 1, 2000, 202609040401, NOW(), NOW(), 1, 1, 0),
(202609040602, 1, 2000, 202609040402, NOW(), NOW(), 1, 1, 0),
(202609040603, 1, 2000, 202609040403, NOW(), NOW(), 1, 1, 0),
(202609040604, 1, 2000, 202609040404, NOW(), NOW(), 1, 1, 0),
(202609040605, 1, 2000, 202609040405, NOW(), NOW(), 1, 1, 0),
(202609040606, 1, 2000, 202609040406, NOW(), NOW(), 1, 1, 0),
(202609040607, 1, 2000, 202609040407, NOW(), NOW(), 1, 1, 0),
(202609040608, 1, 2000, 202609040408, NOW(), NOW(), 1, 1, 0),
(202609040609, 1, 2000, 202609040409, NOW(), NOW(), 1, 1, 0),
(202609040610, 1, 2000, 202609040410, NOW(), NOW(), 1, 1, 0);
