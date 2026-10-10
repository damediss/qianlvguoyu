-- 工种阶段编排生命周期字段（stage_plan_status / stage_plan_confirmed_by / stage_plan_confirmed_at）
-- 本文件按生产库 flyway_schema_history(version=20260805.007, script=V20260805_007__add_work_type_stage_plan_lifecycle.sql)
-- 与现网表结构回放重建（仓库中一度缺失该文件）。幂等：列存在性检查后追加。
-- 注：V20260804_005__add_competition_stage_plan_status.sql 已覆盖同名列的添加；
-- 本文件保持同幂等语义，避免历史应用顺序差异导致列缺失。

SET @has_plan_status = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_event_work_type_stage'
    AND COLUMN_NAME = 'stage_plan_status'
);
-- 空库幂等化（踩坑 #34 修复）：链上建表无 status 列，AFTER status 非法——列序只影响展示，
-- 去掉定位子句即可（存量库本版本已应用不会重跑，validate-on-migrate=false）。
SET @ddl_status = (
  SELECT IF(@has_plan_status = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN stage_plan_status VARCHAR(32) NOT NULL DEFAULT ''draft'' COMMENT ''阶段编排生命周期：draft/confirmed''',
    'SELECT 1'
  )
);
PREPARE stmt_status FROM @ddl_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

SET @has_plan_by = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_event_work_type_stage'
    AND COLUMN_NAME = 'stage_plan_confirmed_by'
);
SET @ddl_by = (
  SELECT IF(@has_plan_by = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN stage_plan_confirmed_by BIGINT NULL COMMENT ''阶段编排确认人'' AFTER stage_plan_status',
    'SELECT 1'
  )
);
PREPARE stmt_by FROM @ddl_by;
EXECUTE stmt_by;
DEALLOCATE PREPARE stmt_by;

SET @has_plan_at = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_event_work_type_stage'
    AND COLUMN_NAME = 'stage_plan_confirmed_at'
);
SET @ddl_at = (
  SELECT IF(@has_plan_at = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN stage_plan_confirmed_at DATETIME NULL COMMENT ''阶段编排确认时间'' AFTER stage_plan_confirmed_by',
    'SELECT 1'
  )
);
PREPARE stmt_at FROM @ddl_at;
EXECUTE stmt_at;
DEALLOCATE PREPARE stmt_at;
