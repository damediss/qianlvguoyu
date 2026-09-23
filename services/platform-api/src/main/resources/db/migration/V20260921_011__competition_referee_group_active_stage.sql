-- 裁判小组记录组长当前切换的赛段任务，组员端只展示该赛段。
-- 取号：已有 V20260921_010，本版本 20260921_011。

SET @db := DATABASE();

SET @add_active_stage_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group
       ADD COLUMN active_stage_id BIGINT NULL
         COMMENT ''组长当前切换的赛段任务ID'' AFTER status',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @db
    AND table_name = 'competition_referee_group'
    AND column_name = 'active_stage_id'
);
PREPARE add_active_stage_stmt FROM @add_active_stage_sql;
EXECUTE add_active_stage_stmt;
DEALLOCATE PREPARE add_active_stage_stmt;
