-- Apply both effects after the test and production migration histories diverged at V20260924_001.
-- Both operations are idempotent so this runs safely after either historical branch.
CREATE TABLE IF NOT EXISTS certification_defense_absence (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  candidate_id BIGINT NOT NULL COMMENT '缺考考生id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_defense_absence (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_defense_absence_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综合答辩缺考记录';

SET @db := DATABASE();
SET @has_status := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_exam_review' AND COLUMN_NAME = 'status');

SET @sql := IF(@has_status > 0,
  'INSERT INTO train_exam_review (exam_id, paper_id, num, user_id, tenant_id, status, sort_number)
   SELECT s.exam_id, 1, 0, 0, COALESCE(NULLIF(s.tenant_id, 0), 10159), 1, 1
   FROM train_class_review_sync s
   WHERE s.deleted = 0 AND s.enabled = 1 AND s.exam_id IS NOT NULL AND s.exam_id > 0
     AND NOT EXISTS (SELECT 1 FROM train_exam_review r WHERE r.exam_id = s.exam_id)',
  'INSERT INTO train_exam_review (exam_id, paper_id, num, user_id, tenant_id)
   SELECT s.exam_id, 1, 0, 0, COALESCE(NULLIF(s.tenant_id, 0), 10159)
   FROM train_class_review_sync s
   WHERE s.deleted = 0 AND s.enabled = 1 AND s.exam_id IS NOT NULL AND s.exam_id > 0
     AND NOT EXISTS (SELECT 1 FROM train_exam_review r WHERE r.exam_id = s.exam_id)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
