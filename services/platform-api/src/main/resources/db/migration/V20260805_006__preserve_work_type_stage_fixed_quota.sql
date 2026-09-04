-- 保留工种阶段固定配额列（promotion_quota）：防止旧数据在阶段规则迁移中丢失配额语义
-- 本文件按生产库 flyway_schema_history(version=20260805.006, script=V20260805_006__preserve_work_type_stage_fixed_quota.sql)
-- 与现网表结构回放重建（仓库中一度缺失该文件）。幂等：列存在性检查后追加。

-- 固定晋级配额列：competition_event_work_type_stage.promotion_quota
-- （百分比制/席位制规则共用阶段表；固定配额行用 promotion_quota 存人数上限）
SET @has_promotion_quota = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_event_work_type_stage'
    AND COLUMN_NAME = 'promotion_quota'
);
SET @ddl_quota = (
  SELECT IF(@has_promotion_quota = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN promotion_quota INT NULL COMMENT ''固定晋级配额（人数上限，QUOTA 规则专用）'' AFTER promotion_percentage',
    'SELECT 1'
  )
);
PREPARE stmt_quota FROM @ddl_quota;
EXECUTE stmt_quota;
DEALLOCATE PREPARE stmt_quota;

-- 同保留目标：固定配额数据的百分比列允许为空（百分比/配额二选一）
SET @ddl_pct_nullable = (
  SELECT IF(@has_promotion_quota = 0,
    'ALTER TABLE competition_event_work_type_stage MODIFY COLUMN promotion_percentage INT NULL COMMENT ''晋级百分比''',
    'SELECT 1'
  )
);
PREPARE stmt_pct FROM @ddl_pct_nullable;
EXECUTE stmt_pct;
DEALLOCATE PREPARE stmt_pct;
