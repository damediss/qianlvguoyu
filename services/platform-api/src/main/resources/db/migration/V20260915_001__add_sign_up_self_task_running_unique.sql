-- 禁止同一报名计划同时存在多个进行中(endtime=0)的导出任务
-- 多实例下仅靠 runningExportTaskIds（进程内）挡不住并发写入，用 DB 唯一键兜底
-- MySQL 不支持部分唯一索引（WHERE），用生成列：进行中 = sign_up_id，已结束 = NULL（NULL 不冲突）
-- 幂等：双库（gjjddb2026 / gjjddbpro）可重复执行

SET @db := DATABASE();

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'train_sign_up_selftask'
        AND COLUMN_NAME = 'running_key'
    ),
    'SELECT 1',
    'ALTER TABLE train_sign_up_selftask ADD COLUMN running_key BIGINT GENERATED ALWAYS AS (IF(endtime = 0, sign_up_id, NULL)) STORED COMMENT ''进行中任务唯一键(endtime=0时=sign_up_id)'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'train_sign_up_selftask'
        AND INDEX_NAME = 'uk_sign_up_self_task_running'
    ),
    'SELECT 1',
    'CREATE UNIQUE INDEX uk_sign_up_self_task_running ON train_sign_up_selftask (running_key)'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
