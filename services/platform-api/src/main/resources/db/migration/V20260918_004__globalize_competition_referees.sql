-- 将裁判档案提升为租户级主数据：身份只创建一次，赛事成员关系按赛事复用。
-- 赛事工种使用 exam_work_type，等级使用统一等级编码；具体赛事/赛段仍由绑定时校验。

SET @referee_person_id_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD COLUMN person_id BIGINT NULL COMMENT ''全局自然人身份ID'' AFTER member_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'person_id'
);
PREPARE referee_person_id_stmt FROM @referee_person_id_sql;
EXECUTE referee_person_id_stmt;
DEALLOCATE PREPARE referee_person_id_stmt;

SET @referee_status_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''active'' COMMENT ''全局裁判档案状态'' AFTER referee_name',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'status'
);
PREPARE referee_status_stmt FROM @referee_status_sql;
EXECUTE referee_status_stmt;
DEALLOCATE PREPARE referee_status_stmt;

SET @referee_member_nullable_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_profile MODIFY COLUMN member_id BIGINT NULL COMMENT ''历史赛事成员ID（新档案不绑定单一赛事）''')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'member_id'
);
PREPARE referee_member_nullable_stmt FROM @referee_member_nullable_sql;
EXECUTE referee_member_nullable_stmt;
DEALLOCATE PREPARE referee_member_nullable_stmt;

SET @referee_person_index_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD KEY idx_competition_referee_profile_person (tenant_id, person_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND index_name = 'idx_competition_referee_profile_person'
);
PREPARE referee_person_index_stmt FROM @referee_person_index_sql;
EXECUTE referee_person_index_stmt;
DEALLOCATE PREPARE referee_person_index_stmt;

CREATE TABLE IF NOT EXISTS competition_referee_qualification (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  referee_profile_id BIGINT NOT NULL,
  work_type_id BIGINT NOT NULL COMMENT '全局题库工种 exam_work_type.id',
  level_code VARCHAR(16) NOT NULL COMMENT '统一工种等级编码1-5',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_referee_qualification_level CHECK (level_code IN ('1', '2', '3', '4', '5')),
  UNIQUE KEY uk_competition_referee_qualification (
    tenant_id, referee_profile_id, work_type_id, level_code, deleted
  ),
  KEY idx_competition_referee_qualification_scope (
    tenant_id, work_type_id, level_code, deleted
  ),
  KEY idx_competition_referee_qualification_profile (
    tenant_id, referee_profile_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='租户级裁判工种等级资质';

-- Backfill the global person reference from the historical event member relation.
UPDATE competition_referee_profile p
JOIN competition_event_member m
  ON m.tenant_id = p.tenant_id
 AND m.id = p.member_id
 AND m.role_type = 'referee'
 AND m.deleted = 0
SET p.person_id = m.person_id
WHERE p.person_id IS NULL
  AND p.deleted = 0;

UPDATE competition_referee_profile
SET status = 'active'
WHERE status IS NULL OR status = '';

-- Preserve historical event-scoped qualifications in the new global qualification table.
SET @next_referee_qualification_id := (
  SELECT COALESCE(MAX(id), 0) FROM competition_referee_qualification
);
INSERT IGNORE INTO competition_referee_qualification (
  id, tenant_id, referee_profile_id, work_type_id, level_code,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  @next_referee_qualification_id := @next_referee_qualification_id + 1,
  p.tenant_id,
  p.id,
  ewt.work_type_id,
  lvl.level_code,
  p.created_by,
  p.updated_by,
  p.created_at,
  p.updated_at,
  0
FROM competition_referee_profile p
JOIN competition_event_member m
  ON m.tenant_id = p.tenant_id
 AND m.id = p.member_id
 AND m.role_type = 'referee'
 AND m.deleted = 0
JOIN competition_event_work_type ewt
  ON ewt.tenant_id = p.tenant_id
 AND ewt.event_id = m.event_id
 AND ewt.id = p.event_work_type_id
 AND ewt.deleted = 0
JOIN competition_event_work_type_level lvl
  ON lvl.tenant_id = p.tenant_id
 AND lvl.event_id = m.event_id
 AND lvl.event_work_type_id = ewt.id
 AND lvl.id = p.event_work_type_level_id
 AND lvl.deleted = 0
WHERE p.person_id IS NOT NULL
  AND p.deleted = 0;

-- If the old event-scoped implementation created the same person more than once,
-- keep the oldest profile, merge its qualifications, and retain event members and
-- assignment history because assignments point to event members rather than profiles.
DROP TEMPORARY TABLE IF EXISTS tmp_competition_referee_keeper;
CREATE TEMPORARY TABLE tmp_competition_referee_keeper AS
SELECT tenant_id, person_id, MIN(id) AS keeper_profile_id
FROM competition_referee_profile
WHERE person_id IS NOT NULL AND deleted = 0
GROUP BY tenant_id, person_id;

SET @next_referee_qualification_id := (
  SELECT COALESCE(MAX(id), 0) FROM competition_referee_qualification
);
INSERT IGNORE INTO competition_referee_qualification (
  id, tenant_id, referee_profile_id, work_type_id, level_code,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  @next_referee_qualification_id := @next_referee_qualification_id + 1,
  q.tenant_id,
  k.keeper_profile_id,
  q.work_type_id,
  q.level_code,
  q.created_by,
  q.updated_by,
  q.created_at,
  q.updated_at,
  0
FROM competition_referee_qualification q
JOIN tmp_competition_referee_keeper k
  ON k.tenant_id = q.tenant_id
JOIN competition_referee_profile duplicate_profile
  ON duplicate_profile.tenant_id = q.tenant_id
 AND duplicate_profile.person_id = k.person_id
 AND duplicate_profile.id = q.referee_profile_id
 AND duplicate_profile.id <> k.keeper_profile_id
WHERE q.deleted = 0;

UPDATE competition_referee_profile duplicate_profile
JOIN tmp_competition_referee_keeper k
  ON k.tenant_id = duplicate_profile.tenant_id
 AND k.person_id = duplicate_profile.person_id
SET duplicate_profile.deleted = 1,
    duplicate_profile.person_id = NULL,
    duplicate_profile.status = 'withdrawn',
    duplicate_profile.updated_at = NOW()
WHERE duplicate_profile.id <> k.keeper_profile_id
  AND duplicate_profile.deleted = 0;

DROP TEMPORARY TABLE tmp_competition_referee_keeper;

-- A soft-deleted historical profile is no longer the global identity owner.
-- Clearing its nullable person reference also keeps the tombstone compatible
-- with the repository's (tenant, person, deleted) uniqueness convention.
UPDATE competition_referee_profile
SET person_id = NULL
WHERE deleted = 1
  AND person_id IS NOT NULL;

SET @referee_person_unique_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD UNIQUE KEY uk_competition_referee_profile_person (tenant_id, person_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND index_name = 'uk_competition_referee_profile_person'
);
PREPARE referee_person_unique_stmt FROM @referee_person_unique_sql;
EXECUTE referee_person_unique_stmt;
DEALLOCATE PREPARE referee_person_unique_stmt;
