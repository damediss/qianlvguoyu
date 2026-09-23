-- 同一赛事、工种、等级、内容类型只允许一份草稿或启用细目表。
-- 归档历史版本不占唯一键。已存在的重复启用/草稿保留版本号最大的启用记录（无启用则保留最新草稿）。
-- 取号：仓库已有到 V20260921_031；本版本 20260922_040 相对双库基线留间隔。

-- 同一范围多份草稿/启用时，优先保留启用中的最大版本，否则保留草稿中的最大版本。
UPDATE competition_question_blueprint t
INNER JOIN (
  SELECT tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type,
         COALESCE(
           MAX(CASE WHEN status = 'active' THEN version_no END),
           MAX(version_no)
         ) AS keep_version,
         CASE
           WHEN MAX(CASE WHEN status = 'active' THEN version_no END) IS NOT NULL THEN 'active'
           ELSE 'draft'
         END AS keep_status
    FROM competition_question_blueprint
   WHERE deleted = 0
     AND status IN ('draft', 'active')
   GROUP BY tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type
  HAVING COUNT(*) > 1
) d
   ON t.tenant_id = d.tenant_id
  AND t.event_id = d.event_id
  AND t.event_work_type_id = d.event_work_type_id
  AND t.event_work_type_level_id = d.event_work_type_level_id
  AND t.content_type = d.content_type
SET t.status = 'archived',
    t.updated_at = NOW()
WHERE t.deleted = 0
  AND t.status IN ('draft', 'active')
  AND NOT (t.version_no = d.keep_version AND t.status = d.keep_status);

-- Keep this migration free of PREPARE/EXECUTE so it also works through the
-- configured Flyway connection.  Flyway records the migration, so the DDL is
-- intentionally a single-run ADD COLUMN rather than dynamic SQL.
ALTER TABLE competition_question_blueprint
  ADD COLUMN scope_live_key TINYINT
    GENERATED ALWAYS AS (
      CASE WHEN deleted = 0 AND status IN ('draft', 'active') THEN 1 ELSE NULL END
    ) STORED
    COMMENT '同一范围仅一份草稿或启用细目表';

ALTER TABLE competition_question_blueprint
  ADD UNIQUE KEY uk_competition_blueprint_scope_live (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, scope_live_key
  );
