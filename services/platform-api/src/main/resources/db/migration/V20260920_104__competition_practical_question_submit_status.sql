-- 竞赛实操评分改为按题提交：评分明细记录评分中/已提交，避免开考后整卷默认分被当成已交。
-- 版本号衔接当前仓库最大脚本 V20260920_103。

SET @add_entry_status_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_practical_score_entry ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''scoring'' COMMENT ''scoring=评分中 submitted=已提交'' AFTER remark',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_practical_score_entry'
    AND column_name = 'status'
);
PREPARE add_entry_status_stmt FROM @add_entry_status_sql;
EXECUTE add_entry_status_stmt;
DEALLOCATE PREPARE add_entry_status_stmt;

UPDATE competition_practical_score_entry entry_row
JOIN competition_practical_score_sheet sheet_row
  ON sheet_row.id = entry_row.score_sheet_id
 AND sheet_row.deleted = 0
SET entry_row.status = 'submitted',
    entry_row.updated_at = NOW()
WHERE entry_row.deleted = 0
  AND sheet_row.status = 'submitted_locked'
  AND (entry_row.status IS NULL OR entry_row.status <> 'submitted');
