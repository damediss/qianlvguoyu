-- 班级同步学开通后，考试计划须有 train_exam_review 行，App 才会归入「复习任务」并显示「同步练习」。
-- 历史数据：名单/灰度已写、复习配置缺失 → 本脚本补齐。
-- 空库幂等化（踩坑 #34 修复）：status / sort_number 是存量库 legacy 列，链上精简建表
-- （V20260521_001）没有——legacy 形态走原语句，空库走精简语句。存量库本版本已应用不会重跑。
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
