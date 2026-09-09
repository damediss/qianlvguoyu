-- 方案 B（2026-09-09 生产事故修复）：task_exam 族与 PHP 老系统共用，addtime 列对齐 PHP 的 int 时间戳口径
-- 事故：生产 task_exam.addtime 为 INT，实体按 LocalDateTime 映射，列表查询报
--       "Unsupported conversion from LONG to java.time.LocalDateTime"（认定考核任务页整页 500）。
-- 本迁移仅当列仍为 datetime/timestamp 时转换为 INT（存量值 UNIX_TIMESTAMP 换算）；
-- 生产库该列已是 INT 时三步全部跳过（幂等）。空库全量回放：V20260528_006 建 datetime → 本迁移转 INT。
-- 涉及 6 张表：task_exam / task_examshicao / task_examsub + 3 张 audit_records（ximu/expert 族未确认，暂不动）。
-- 版本号依据（2026-09-09）：仓库工作区最大 V20260908_011；gjjddb2026(测试) MAX=20260908.003（踩坑 #7）。

-- 1) task_exam
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_exam' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_exam ADD COLUMN addtime_ts INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_exam SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_exam DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) task_examshicao
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examshicao' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examshicao ADD COLUMN addtime_ts INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_examshicao SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examshicao DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) task_examsub
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examsub ADD COLUMN addtime_ts INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_examsub SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examsub DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''创建时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) task_exam_audit_records
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_exam_audit_records' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_exam_audit_records ADD COLUMN addtime_ts INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_exam_audit_records SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_exam_audit_records DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) task_examshicao_audit_records
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examshicao_audit_records' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examshicao_audit_records ADD COLUMN addtime_ts INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_examshicao_audit_records SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examshicao_audit_records DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) task_examsub_audit_records
SET @is_dt = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_audit_records' AND COLUMN_NAME = 'addtime'
    AND DATA_TYPE IN ('datetime','timestamp'));
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examsub_audit_records ADD COLUMN addtime_ts INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'UPDATE task_examsub_audit_records SET addtime_ts = UNIX_TIMESTAMP(addtime) WHERE addtime IS NOT NULL',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(@is_dt = 1,
  'ALTER TABLE task_examsub_audit_records DROP COLUMN addtime, CHANGE COLUMN addtime_ts addtime INT NULL COMMENT ''提交时间（Unix 秒，PHP 口径）''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
