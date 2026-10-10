-- 竞赛实操题结构化详情与评分表。
-- 不修改已应用迁移；旧字段保留用于兼容历史快照和导入客户端。
-- MySQL DDL 自动提交：本脚本对列/索引/约束做 information_schema 守卫，支持失败后重跑。

-- ---------- competition_practical_question_detail ----------
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN duration_minutes INT NULL COMMENT ''考试时间（分钟）'' AFTER question_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'duration_minutes'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN score_difference DECIMAL(12,2) NULL COMMENT ''裁判分差阈值'' AFTER duration_minutes',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'score_difference'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN total_score DECIMAL(12,2) NULL COMMENT ''实操题总分'' AFTER score_difference',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'total_score'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN scoring_method VARCHAR(16) NULL COMMENT ''additive-加分制，deductive-扣分制'' AFTER total_score',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'scoring_method'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN assessment_requirements_html LONGTEXT NULL COMMENT ''考核要求富文本'' AFTER scoring_method',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'assessment_requirements_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN exam_room_prepare_html LONGTEXT NULL COMMENT ''考场准备富文本'' AFTER assessment_requirements_html',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'exam_room_prepare_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN candidate_prepare_html LONGTEXT NULL COMMENT ''考生准备富文本'' AFTER exam_room_prepare_html',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'candidate_prepare_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN negative_items_html LONGTEXT NULL COMMENT ''否定项富文本'' AFTER candidate_prepare_html',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'negative_items_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD COLUMN question_materials_html LONGTEXT NULL COMMENT ''试题材料富文本'' AFTER negative_items_html',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND column_name = 'question_materials_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_detail ADD KEY idx_competition_practical_detail_question_v2 (tenant_id, question_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_detail'
    AND index_name = 'idx_competition_practical_detail_question_v2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- competition_practical_question_assessment_content ----------
CREATE TABLE IF NOT EXISTS competition_practical_question_assessment_content (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  content_html LONGTEXT NOT NULL COMMENT '考核内容富文本',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_practical_content_order CHECK (sort_order > 0),
  UNIQUE KEY uk_competition_practical_content_order (tenant_id, question_id, sort_order, deleted),
  KEY idx_competition_practical_content_question (tenant_id, question_id, sort_order, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操题考核内容分组';

-- ---------- competition_practical_question_score_item ----------
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD COLUMN content_id BIGINT NULL COMMENT ''考核内容ID'' AFTER question_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'content_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD COLUMN element_order INT NULL COMMENT ''内容内评分要素序号'' AFTER content_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'element_order'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD COLUMN element_html LONGTEXT NULL COMMENT ''评分要素富文本'' AFTER item_name',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'element_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD COLUMN allocation_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''是否配分'' AFTER element_html',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'allocation_enabled'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD COLUMN standard_html LONGTEXT NULL COMMENT ''评分标准富文本'' AFTER criterion',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'standard_html'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_practical_question_score_item MODIFY COLUMN max_score DECIMAL(12,2) NULL')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'max_score'
    AND is_nullable = 'NO'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_practical_question_score_item MODIFY COLUMN max_deduction DECIMAL(12,2) NULL')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'max_deduction'
    AND is_nullable = 'NO'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_practical_question_score_item MODIFY COLUMN deduction_step DECIMAL(12,2) NULL')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND column_name = 'deduction_step'
    AND is_nullable = 'NO'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_practical_question_score_item DROP CHECK chk_competition_practical_item_scores')
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND constraint_name = 'chk_competition_practical_item_scores'
    AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD CONSTRAINT chk_competition_practical_item_allocation CHECK (
      (allocation_enabled = 0 AND max_score IS NULL)
      OR (allocation_enabled = 1 AND max_score > 0 AND max_score <= 100)
    )',
    'SELECT 1')
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND constraint_name = 'chk_competition_practical_item_allocation'
    AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD CONSTRAINT chk_competition_practical_item_deduction_values CHECK (
      (max_deduction IS NULL AND deduction_step IS NULL)
      OR (max_deduction IS NOT NULL AND deduction_step IS NOT NULL
          AND max_deduction >= 0 AND max_deduction <= 100 AND deduction_step > 0)
    )',
    'SELECT 1')
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND constraint_name = 'chk_competition_practical_item_deduction_values'
    AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD CONSTRAINT chk_competition_practical_item_element_order CHECK (
      element_order IS NULL OR element_order > 0
    )',
    'SELECT 1')
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND constraint_name = 'chk_competition_practical_item_element_order'
    AND constraint_type = 'CHECK'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD KEY idx_competition_practical_item_content (tenant_id, content_id, element_order, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND index_name = 'idx_competition_practical_item_content'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_question_score_item ADD UNIQUE KEY uk_competition_practical_item_content_order (tenant_id, content_id, element_order, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_question_score_item'
    AND index_name = 'uk_competition_practical_item_content_order'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- competition_make_exam_plan ----------
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_plan ADD COLUMN practical_question_id BIGINT NULL AFTER event_work_type_level_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_plan'
    AND column_name = 'practical_question_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_plan ADD KEY idx_competition_make_plan_practical_question (tenant_id, practical_question_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_plan'
    AND index_name = 'idx_competition_make_plan_practical_question'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- competition_make_exam_paper_question ----------
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_paper_question ADD COLUMN practical_detail_json JSON NULL AFTER scoring_items_json',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_paper_question'
    AND column_name = 'practical_detail_json'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_paper_question ADD COLUMN score_difference DECIMAL(12,2) NULL AFTER practical_detail_json',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_paper_question'
    AND column_name = 'score_difference'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_paper_question ADD COLUMN scoring_method VARCHAR(16) NULL AFTER score_difference',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_paper_question'
    AND column_name = 'scoring_method'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_make_exam_paper_question ADD KEY idx_competition_make_snapshot_question_practical (tenant_id, source_question_id, question_type, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_make_exam_paper_question'
    AND index_name = 'idx_competition_make_snapshot_question_practical'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Existing flat score items become elements in a default content group. The question ID is
-- deliberately reused as the content ID: IDs are scoped to their own table and this keeps the
-- migration deterministic without introducing a database-specific ID generator.
INSERT IGNORE INTO competition_practical_question_assessment_content (
  id, tenant_id, question_id, sort_order, content_html,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT d.question_id, d.tenant_id, d.question_id, 1,
       COALESCE(NULLIF(d.operation_content, ''), '<p>默认考核内容</p>'),
       d.created_by, d.updated_by, d.created_at, d.updated_at, d.deleted
FROM competition_practical_question_detail d
WHERE d.deleted = 0;

UPDATE competition_practical_question_detail d
SET d.assessment_requirements_html = COALESCE(d.assessment_requirements_html, d.operation_content),
    d.exam_room_prepare_html = COALESCE(d.exam_room_prepare_html, d.equipment_prepare),
    d.candidate_prepare_html = COALESCE(d.candidate_prepare_html, d.student_prepare),
    d.duration_minutes = COALESCE(d.duration_minutes, 60),
    d.score_difference = COALESCE(d.score_difference, LEAST(10, COALESCE(d.total_score, (
      SELECT COALESCE(SUM(i2.max_score), 0)
      FROM competition_practical_question_score_item i2
      WHERE i2.tenant_id = d.tenant_id AND i2.question_id = d.question_id AND i2.deleted = 0
    )))),
    d.scoring_method = COALESCE(d.scoring_method, 'deductive'),
    d.total_score = COALESCE(d.total_score, (
      SELECT COALESCE(SUM(i.max_score), 0)
      FROM competition_practical_question_score_item i
      WHERE i.tenant_id = d.tenant_id AND i.question_id = d.question_id AND i.deleted = 0
    ));

UPDATE competition_practical_question_score_item i
SET i.content_id = i.question_id,
    i.element_order = i.item_no,
    i.element_html = COALESCE(i.element_html, i.item_name),
    i.standard_html = COALESCE(i.standard_html, NULLIF(i.criterion, ''), i.item_name),
    i.allocation_enabled = 1;

-- Keep the legacy question-level score column aligned for readers that still
-- project it; the structured detail remains the authoritative source for new
-- practical plans and snapshots.
UPDATE competition_question q
JOIN competition_practical_question_detail d
  ON d.tenant_id = q.tenant_id AND d.question_id = q.id AND d.deleted = 0
SET q.score = d.total_score
WHERE q.content_type = 'practical' AND q.deleted = 0 AND d.total_score > 0;

-- The old item_name/criterion columns remain populated for legacy readers. New application
-- writes keep them synchronized with the structured HTML aliases.
