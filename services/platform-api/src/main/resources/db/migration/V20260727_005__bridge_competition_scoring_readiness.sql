-- COMP-SCORE-01: establish stable event work-type and score-rule keys before scoring.
CREATE TABLE competition_event_work_type (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  work_type_id BIGINT NOT NULL COMMENT '关联当前租户 exam_work_type.id',
  display_name VARCHAR(255) NOT NULL COMMENT '赛事内工种显示快照',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_event_work_type_status CHECK (
    status IN ('active', 'inactive')
  ),
  CONSTRAINT chk_competition_event_work_type_sort_order CHECK (sort_order >= 0),
  UNIQUE KEY uk_competition_event_work_type (
    tenant_id, event_id, work_type_id, deleted
  ),
  KEY idx_competition_event_work_type_event_status (
    tenant_id, event_id, status, sort_order, deleted
  ),
  KEY idx_competition_event_work_type_source (
    tenant_id, work_type_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛事启用的规范化题库工种';

ALTER TABLE competition_contestant_profile
  ADD COLUMN event_work_type_id BIGINT NULL COMMENT '已人工确认的赛事工种' AFTER current_stage_id,
  ADD COLUMN work_type_match_status VARCHAR(32) NOT NULL DEFAULT 'unmatched'
    COMMENT '名单工种对账状态' AFTER event_work_type_id,
  ADD COLUMN work_type_match_note VARCHAR(500) NULL COMMENT '对账或人工确认说明'
    AFTER work_type_match_status,
  ADD CONSTRAINT chk_competition_contestant_work_type_match_status CHECK (
    work_type_match_status IN ('matched', 'unmatched', 'conflict')
  ),
  ADD KEY idx_competition_contestant_event_work_type (
    tenant_id, event_work_type_id, work_type_match_status, deleted
  );

CREATE TABLE competition_score_rule (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  scoring_system VARCHAR(16) NOT NULL DEFAULT 'B',
  theory_template VARCHAR(32) NULL,
  theory_weight DECIMAL(5,2) NULL,
  simulation_practical_weight DECIMAL(5,2) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  rule_version INT NOT NULL,
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_score_rule_system CHECK (scoring_system = 'B'),
  CONSTRAINT chk_competition_score_rule_template CHECK (
    theory_template IS NULL OR theory_template IN ('THEORY_90', 'THEORY_120')
  ),
  CONSTRAINT chk_competition_score_rule_published_values CHECK (
    status <> 'published'
    OR (
      theory_template IS NOT NULL
      AND theory_weight IS NOT NULL
      AND simulation_practical_weight IS NOT NULL
      AND theory_weight >= 0.00
      AND theory_weight <= 100.00
      AND simulation_practical_weight >= 0.00
      AND simulation_practical_weight <= 100.00
      AND theory_weight + simulation_practical_weight = 100.00
    )
  ),
  CONSTRAINT chk_competition_score_rule_status CHECK (
    status IN ('draft', 'published', 'deprecated')
  ),
  CONSTRAINT chk_competition_score_rule_version CHECK (rule_version > 0),
  UNIQUE KEY uk_competition_score_rule_version (
    tenant_id, event_id, stage_id, event_work_type_id, rule_version, deleted
  ),
  KEY idx_competition_score_rule_scope_status (
    tenant_id, event_id, stage_id, event_work_type_id, status, deleted
  ),
  KEY idx_competition_score_rule_published (
    tenant_id, event_id, status, published_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛制B评分规则版本';

-- Reactivate the stage and registration permissions retired before their API was ready.
UPDATE sys_permission
SET status = 'enabled',
    deleted = 0,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code IN (
    'competition:stage:view',
    'competition:stage:manage',
    'competition:registration:view',
    'competition:registration:audit'
  );

SET @competition_event_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = 1
    AND code = 'competition_event'
    AND deleted = 0
  LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9968, 1, @competition_event_menu_id, 'competition:event-work-type:view',
   '查看赛事工种', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9969, 1, @competition_event_menu_id, 'competition:event-work-type:manage',
   '管理赛事工种', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9970, 1, @competition_event_menu_id, 'competition:score-rule:view',
   '查看竞赛评分规则', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9971, 1, @competition_event_menu_id, 'competition:score-rule:manage',
   '管理竞赛评分规则', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9972, 1, @competition_event_menu_id, 'competition:scoring-readiness:view',
   '查看竞赛评分就绪状态', 'view', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

-- Ensure all restored and new capabilities are attached to the existing event menu.
UPDATE sys_permission
SET menu_id = @competition_event_menu_id,
    status = 'enabled',
    deleted = 0,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code IN (
    'competition:stage:view',
    'competition:stage:manage',
    'competition:registration:view',
    'competition:registration:audit',
    'competition:event-work-type:view',
    'competition:event-work-type:manage',
    'competition:score-rule:view',
    'competition:score-rule:manage',
    'competition:scoring-readiness:view'
  );

-- Preserve existing role grants for restored permissions and grant all new
-- scoring-readiness capabilities to the platform administrator role.
SET @next_competition_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_role_permission_id := @next_competition_role_permission_id + 1,
  p.tenant_id,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'competition:stage:view',
    'competition:stage:manage',
    'competition:registration:view',
    'competition:registration:audit',
    'competition:event-work-type:view',
    'competition:event-work-type:manage',
    'competition:score-rule:view',
    'competition:score-rule:manage',
    'competition:scoring-readiness:view'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
