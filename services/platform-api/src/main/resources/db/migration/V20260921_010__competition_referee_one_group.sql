-- 一名裁判同一时间只能属于一个未删除的有效裁判小组。
-- 软删后生成列为 NULL，历史解散记录不占唯一键。
-- 取号：测试库 MAX=20260920.107，生产库 MAX=20260921.001，本版本 20260921_010 留间隔。

SET @db := DATABASE();

-- 若同一裁判已在多个有效小组中，只保留最早一条，其余退出小组。
UPDATE competition_referee_group_member m
JOIN (
  SELECT tenant_id, referee_profile_id, MIN(id) AS keep_id
    FROM competition_referee_group_member
   WHERE deleted = 0
     AND status = 'active'
     AND referee_profile_id IS NOT NULL
   GROUP BY tenant_id, referee_profile_id
  HAVING COUNT(*) > 1
) dup
  ON dup.tenant_id = m.tenant_id
 AND dup.referee_profile_id = m.referee_profile_id
SET m.status = 'removed',
    m.deleted = 1,
    m.updated_at = NOW()
WHERE m.deleted = 0
  AND m.status = 'active'
  AND m.referee_profile_id IS NOT NULL
  AND m.id <> dup.keep_id;

SET @add_col_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group_member
       ADD COLUMN active_referee_profile_id BIGINT
         GENERATED ALWAYS AS (
           CASE WHEN deleted = 0 AND status = ''active'' THEN referee_profile_id ELSE NULL END
         ) STORED
         COMMENT ''有效小组成员档案ID，用于一人一组唯一约束''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @db
    AND table_name = 'competition_referee_group_member'
    AND column_name = 'active_referee_profile_id'
);
PREPARE add_col_stmt FROM @add_col_sql;
EXECUTE add_col_stmt;
DEALLOCATE PREPARE add_col_stmt;

SET @add_uk_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group_member
       ADD UNIQUE KEY uk_comp_referee_one_group (tenant_id, active_referee_profile_id)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = @db
    AND table_name = 'competition_referee_group_member'
    AND index_name = 'uk_comp_referee_one_group'
);
PREPARE add_uk_stmt FROM @add_uk_sql;
EXECUTE add_uk_stmt;
DEALLOCATE PREPARE add_uk_stmt;
