-- 导出任务记录实际 zip 相对路径，check 按库字段取包，避免按标题猜路径失败
-- 幂等：双库（gjjddb2026 / gjjddbpro）可重复执行

SET @db := DATABASE();

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'train_sign_up_selftask'
        AND COLUMN_NAME = 'result_file'
    ),
    'SELECT 1',
    'ALTER TABLE train_sign_up_selftask ADD COLUMN result_file VARCHAR(512) NULL COMMENT ''成功时的 zip 相对路径'' AFTER error_msg'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
