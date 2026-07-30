-- Competition personnel is globally unique; roles are scoped to a competition event.
-- The historical competition_player_identity name is retained for API compatibility.
ALTER TABLE competition_player_identity
  COMMENT = '竞赛全局自然人安全身份（敏感字段加密存储，兼容原选手身份表名）';

CREATE TABLE competition_event_member (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  person_id BIGINT NOT NULL COMMENT '关联 competition_player_identity.id',
  role_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  joined_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_event_member_role CHECK (
    role_type IN ('contestant', 'expert', 'referee', 'event_admin', 'staff')
  ),
  CONSTRAINT chk_competition_event_member_status CHECK (
    status IN ('active', 'suspended', 'withdrawn')
  ),
  UNIQUE KEY uk_competition_event_member_role (
    tenant_id, event_id, person_id, role_type, deleted
  ),
  KEY idx_competition_event_member_event_role (
    tenant_id, event_id, role_type, status, deleted
  ),
  KEY idx_competition_event_member_person (
    tenant_id, person_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛人员与赛事角色关系';

CREATE TABLE competition_contestant_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  registration_id BIGINT NULL COMMENT '关联历史参赛资格 competition_registration.id',
  contestant_no VARCHAR(64) NULL,
  group_code VARCHAR(64) NULL,
  current_stage_id BIGINT NULL,
  promotion_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  total_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_contestant_profile_promotion CHECK (
    promotion_status IN ('pending', 'promoted', 'eliminated')
  ),
  CONSTRAINT chk_competition_contestant_profile_total_score CHECK (total_score >= 0),
  UNIQUE KEY uk_competition_contestant_profile_member (tenant_id, member_id, deleted),
  UNIQUE KEY uk_competition_contestant_profile_registration (tenant_id, registration_id, deleted),
  UNIQUE KEY uk_competition_contestant_profile_no (tenant_id, contestant_no, deleted),
  KEY idx_competition_contestant_profile_stage (
    tenant_id, current_stage_id, promotion_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛选手角色扩展';

CREATE TABLE competition_expert_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  professional_field VARCHAR(256) NULL,
  expert_level VARCHAR(64) NULL,
  certificate_no VARCHAR(128) NULL,
  review_group_code VARCHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_expert_profile_member (tenant_id, member_id, deleted),
  KEY idx_competition_expert_profile_group (tenant_id, review_group_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛专家角色扩展';

CREATE TABLE competition_referee_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  professional_field VARCHAR(256) NULL,
  referee_level VARCHAR(64) NULL,
  certificate_no VARCHAR(128) NULL,
  referee_group_code VARCHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_referee_profile_member (tenant_id, member_id, deleted),
  KEY idx_competition_referee_profile_group (tenant_id, referee_group_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛裁判角色扩展';

CREATE TABLE competition_member_stage_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  assignment_type VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_member_stage_assignment_type CHECK (
    assignment_type IN ('review', 'score', 'manage')
  ),
  UNIQUE KEY uk_competition_member_stage_assignment (
    tenant_id, member_id, stage_id, assignment_type, deleted
  ),
  KEY idx_competition_member_stage_assignment_stage (
    tenant_id, stage_id, assignment_type, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='专家裁判可操作赛段授权';

CREATE TABLE competition_member_avoidance_org (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_member_avoidance_org (tenant_id, member_id, org_id, deleted),
  KEY idx_competition_member_avoidance_org_org (tenant_id, org_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='专家裁判回避单位';

CREATE TABLE competition_event_role_conflict_rule (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  role_type_a VARCHAR(32) NOT NULL,
  role_type_b VARCHAR(32) NOT NULL,
  resolution VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_event_role_conflict_roles CHECK (
    role_type_a IN ('contestant', 'expert', 'referee', 'event_admin', 'staff')
    AND role_type_b IN ('contestant', 'expert', 'referee', 'event_admin', 'staff')
    AND role_type_a < role_type_b
  ),
  CONSTRAINT chk_competition_event_role_conflict_resolution CHECK (
    resolution IN ('forbidden', 'recusal_required', 'allowed')
  ),
  UNIQUE KEY uk_competition_event_role_conflict_rule (
    tenant_id, event_id, role_type_a, role_type_b, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛事角色冲突与回避规则';

ALTER TABLE competition_registration
  ADD COLUMN member_id BIGINT NULL COMMENT '关联 competition_event_member.id' AFTER player_id,
  ADD KEY idx_competition_registration_member (tenant_id, member_id, deleted);

-- Migrate only registrations that already have a verified natural-person identity.
-- Rows without player_id are intentionally left unlinked for manual remediation.
INSERT INTO competition_event_member (
  id, tenant_id, event_id, person_id, role_type, status, joined_at,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  r.id, r.tenant_id, r.event_id, r.player_id, 'contestant',
  CASE WHEN r.status IN ('rejected', 'withdrawn', 'expired') THEN 'withdrawn' ELSE 'active' END,
  r.created_at, r.created_by, r.updated_by, r.created_at, r.updated_at, r.deleted
FROM competition_registration r
WHERE r.player_id IS NOT NULL
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM competition_event_member m
    WHERE m.tenant_id = r.tenant_id
      AND m.event_id = r.event_id
      AND m.person_id = r.player_id
      AND m.role_type = 'contestant'
      AND m.deleted = 0
  );

UPDATE competition_registration r
JOIN competition_event_member m
  ON m.tenant_id = r.tenant_id
 AND m.event_id = r.event_id
 AND m.person_id = r.player_id
 AND m.role_type = 'contestant'
 AND m.deleted = 0
SET r.member_id = m.id
WHERE r.player_id IS NOT NULL
  AND r.deleted = 0;

INSERT INTO competition_contestant_profile (
  id, tenant_id, member_id, registration_id, promotion_status, total_score,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  r.id, r.tenant_id, r.member_id, r.id, 'pending', 0.00,
  r.created_by, r.updated_by, r.created_at, r.updated_at, r.deleted
FROM competition_registration r
WHERE r.member_id IS NOT NULL
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM competition_contestant_profile p
    WHERE p.tenant_id = r.tenant_id AND p.member_id = r.member_id AND p.deleted = 0
  );

ALTER TABLE competition_registration
  ADD UNIQUE KEY uk_competition_registration_event_member (
    tenant_id, event_id, member_id, deleted
  );
