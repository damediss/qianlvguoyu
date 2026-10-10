-- 将裁判资质等级从单个等级升级为“起始等级-结束等级”范围。
-- 现有单等级资质会迁移为起止等级相同；同一裁判同一工种的历史多等级
-- 资质会合并为一个范围，保留最早记录，其余记录按逻辑删除。

ALTER TABLE competition_referee_qualification
  ADD COLUMN level_from_code VARCHAR(16) NULL COMMENT '等级范围起始编码（低等级端）' AFTER work_type_id,
  ADD COLUMN level_to_code VARCHAR(16) NULL COMMENT '等级范围结束编码（高等级端）' AFTER level_from_code;

UPDATE competition_referee_qualification
SET level_from_code = COALESCE(level_from_code, level_code),
    level_to_code = COALESCE(level_to_code, level_code)
WHERE level_from_code IS NULL OR level_to_code IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_competition_referee_qualification_range;
CREATE TEMPORARY TABLE tmp_competition_referee_qualification_range AS
SELECT tenant_id,
       referee_profile_id,
       work_type_id,
       MIN(id) AS keeper_id,
       MAX(CAST(COALESCE(level_from_code, level_code) AS UNSIGNED)) AS range_from_code,
       MIN(CAST(COALESCE(level_to_code, level_code) AS UNSIGNED)) AS range_to_code
  FROM competition_referee_qualification
 WHERE deleted = 0
 GROUP BY tenant_id, referee_profile_id, work_type_id;

-- 等级编码沿用竞赛模块约定：5=初级工，1=高级技师。
-- 因此“初级工-高级工”存为 from=5、to=3。
UPDATE competition_referee_qualification q
JOIN tmp_competition_referee_qualification_range r
  ON r.tenant_id = q.tenant_id
 AND r.referee_profile_id = q.referee_profile_id
 AND r.work_type_id = q.work_type_id
 AND r.keeper_id = q.id
SET q.level_from_code = CAST(r.range_from_code AS CHAR),
    q.level_to_code = CAST(r.range_to_code AS CHAR),
    q.level_code = CAST(r.range_from_code AS CHAR),
    q.updated_at = NOW();

UPDATE competition_referee_qualification q
JOIN tmp_competition_referee_qualification_range r
  ON r.tenant_id = q.tenant_id
 AND r.referee_profile_id = q.referee_profile_id
 AND r.work_type_id = q.work_type_id
SET q.deleted = 1,
    q.updated_at = NOW()
WHERE q.deleted = 0
  AND q.id <> r.keeper_id;

DROP TEMPORARY TABLE tmp_competition_referee_qualification_range;

ALTER TABLE competition_referee_qualification
  MODIFY COLUMN level_from_code VARCHAR(16) NOT NULL COMMENT '等级范围起始编码（低等级端）',
  MODIFY COLUMN level_to_code VARCHAR(16) NOT NULL COMMENT '等级范围结束编码（高等级端）',
  ADD CONSTRAINT chk_competition_referee_qualification_range_from
    CHECK (level_from_code IN ('1', '2', '3', '4', '5')),
  ADD CONSTRAINT chk_competition_referee_qualification_range_to
    CHECK (level_to_code IN ('1', '2', '3', '4', '5')),
  ADD KEY idx_competition_referee_qualification_range_scope (
    tenant_id, work_type_id, level_from_code, level_to_code, deleted
  );
