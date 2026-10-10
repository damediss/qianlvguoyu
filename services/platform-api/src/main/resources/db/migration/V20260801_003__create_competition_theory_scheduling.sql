-- COMP-SCORE-03C: versioned theory-exam scheduling, rooms, sessions and assignments.

CREATE TABLE competition_theory_schedule_version (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  score_rule_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  random_batch_id VARCHAR(64) NULL,
  random_seed VARCHAR(128) NULL,
  generated_at DATETIME NULL,
  generated_by BIGINT NULL,
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  withdrawn_at DATETIME NULL,
  withdrawn_by BIGINT NULL,
  withdraw_reason VARCHAR(1000) NULL,
  active_scope_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status IN ('draft', 'published') THEN 1 ELSE NULL END
  ) STORED,
  current_published_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_schedule_version CHECK (version_no > 0),
  CONSTRAINT chk_comp_theory_schedule_status CHECK (
    status IN ('draft', 'published', 'withdrawn')
  ),
  UNIQUE KEY uk_comp_theory_schedule_version (
    tenant_id, event_id, stage_id, event_work_type_id, version_no, deleted
  ),
  UNIQUE KEY uk_comp_theory_schedule_active (
    tenant_id, event_id, stage_id, event_work_type_id, active_scope_key
  ),
  UNIQUE KEY uk_comp_theory_schedule_published (
    tenant_id, event_id, stage_id, event_work_type_id, current_published_key
  ),
  KEY idx_comp_theory_schedule_rule (
    tenant_id, score_rule_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论考试整版安排版本';

CREATE TABLE competition_theory_exam_room (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(500) NOT NULL,
  capacity INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  remark VARCHAR(1000) NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_room_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
  CONSTRAINT chk_comp_theory_room_address CHECK (CHAR_LENGTH(TRIM(address)) > 0),
  CONSTRAINT chk_comp_theory_room_capacity CHECK (capacity > 0),
  CONSTRAINT chk_comp_theory_room_status CHECK (status IN ('enabled', 'disabled')),
  UNIQUE KEY uk_comp_theory_room_name (
    tenant_id, event_id, name, active_record_key
  ),
  KEY idx_comp_theory_room_event_status (tenant_id, event_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛事临时理论考场';

CREATE TABLE competition_theory_exam_session (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  schedule_version_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  session_code VARCHAR(16) NOT NULL,
  session_name VARCHAR(128) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  total_capacity INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_session_code CHECK (session_code IN ('A', 'B')),
  CONSTRAINT chk_comp_theory_session_name CHECK (CHAR_LENGTH(TRIM(session_name)) > 0),
  CONSTRAINT chk_comp_theory_session_time CHECK (end_at > start_at),
  CONSTRAINT chk_comp_theory_session_capacity CHECK (total_capacity >= 0),
  CONSTRAINT chk_comp_theory_session_status CHECK (
    status IN ('draft', 'published', 'withdrawn')
  ),
  UNIQUE KEY uk_comp_theory_session_code (
    tenant_id, schedule_version_id, session_code, active_record_key
  ),
  KEY idx_comp_theory_session_time (
    tenant_id, start_at, end_at, status, deleted
  ),
  KEY idx_comp_theory_session_scope (
    tenant_id, event_id, stage_id, event_work_type_id, schedule_version_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论考试场次';

CREATE TABLE competition_theory_session_room (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  schedule_version_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  room_name_snapshot VARCHAR(128) NOT NULL,
  address_snapshot VARCHAR(500) NOT NULL,
  capacity_snapshot INT NOT NULL,
  sort_order INT NOT NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_session_room_capacity CHECK (capacity_snapshot > 0),
  CONSTRAINT chk_comp_theory_session_room_sort CHECK (sort_order > 0),
  UNIQUE KEY uk_comp_theory_session_room (
    tenant_id, session_id, room_id, active_record_key
  ),
  UNIQUE KEY uk_comp_theory_session_room_sort (
    tenant_id, session_id, sort_order, active_record_key
  ),
  KEY idx_comp_theory_session_room_room (
    tenant_id, room_id, schedule_version_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='理论考试场次考场及容量快照';

CREATE TABLE competition_theory_candidate_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  schedule_version_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  session_room_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  player_id BIGINT NULL,
  seat_no VARCHAR(16) NOT NULL,
  random_batch_id VARCHAR(64) NOT NULL,
  allocation_source VARCHAR(32) NOT NULL DEFAULT 'random',
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status IN ('draft', 'published') THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_assignment_seat CHECK (CHAR_LENGTH(TRIM(seat_no)) > 0),
  CONSTRAINT chk_comp_theory_assignment_source CHECK (
    allocation_source IN ('random', 'manual')
  ),
  CONSTRAINT chk_comp_theory_assignment_status CHECK (
    status IN ('draft', 'published', 'withdrawn')
  ),
  UNIQUE KEY uk_comp_theory_assignment_version_candidate (
    tenant_id, schedule_version_id, contestant_profile_id, active_record_key
  ),
  UNIQUE KEY uk_comp_theory_assignment_seat (
    tenant_id, session_room_id, seat_no, active_record_key
  ),
  UNIQUE KEY uk_comp_theory_assignment_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    contestant_profile_id, active_current_key
  ),
  KEY idx_comp_theory_assignment_roster (
    tenant_id, schedule_version_id, session_id, session_room_id, seat_no, deleted
  ),
  KEY idx_comp_theory_assignment_player (
    tenant_id, player_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='理论考试选手场次考场座位分配';

CREATE TABLE competition_theory_assignment_adjustment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  schedule_version_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  from_session_id BIGINT NOT NULL,
  from_session_room_id BIGINT NOT NULL,
  from_seat_no VARCHAR(16) NOT NULL,
  to_session_id BIGINT NOT NULL,
  to_session_room_id BIGINT NOT NULL,
  to_seat_no VARCHAR(16) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  adjusted_by BIGINT NOT NULL,
  adjusted_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_adjustment_reason CHECK (
    CHAR_LENGTH(TRIM(reason)) > 0
  ),
  KEY idx_comp_theory_adjustment_assignment (
    tenant_id, assignment_id, adjusted_at, deleted
  ),
  KEY idx_comp_theory_adjustment_schedule (
    tenant_id, schedule_version_id, contestant_profile_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='理论考试特殊选手人工调整审计';

ALTER TABLE competition_player_message
  MODIFY COLUMN notice_id BIGINT NULL,
  ADD COLUMN schedule_version_id BIGINT NULL AFTER notice_id,
  ADD COLUMN title VARCHAR(256) NULL AFTER type,
  ADD COLUMN content TEXT NULL AFTER title,
  ADD COLUMN published_at DATETIME NULL AFTER content,
  DROP CHECK chk_competition_player_message_type,
  ADD CONSTRAINT chk_competition_player_message_type CHECK (
    type IN ('notice_published', 'theory_schedule_published', 'theory_schedule_withdrawn')
  ),
  ADD CONSTRAINT chk_competition_player_message_reference CHECK (
    (type = 'notice_published' AND notice_id IS NOT NULL AND schedule_version_id IS NULL)
    OR
    (type IN ('theory_schedule_published', 'theory_schedule_withdrawn')
      AND notice_id IS NULL AND schedule_version_id IS NOT NULL
      AND title IS NOT NULL AND content IS NOT NULL AND published_at IS NOT NULL)
  ),
  ADD UNIQUE KEY uk_comp_player_message_schedule (
    tenant_id, schedule_version_id, player_id, type, deleted
  );

SET @competition_event_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_event' AND deleted = 0
  LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9980, 1, @competition_event_menu_id, 'competition:theory-schedule:view',
   '查看理论考试排场', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9981, 1, @competition_event_menu_id, 'competition:theory-schedule:manage',
   '管理理论考试排场', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9982, 1, @competition_event_menu_id, 'competition:theory-schedule:publish',
   '发布理论考试安排', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9983, 1, @competition_event_menu_id, 'competition:theory-roster:export',
   '导出理论考试名单', 'export', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_comp_theory_schedule_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_comp_theory_schedule_role_permission_id :=
    @next_comp_theory_schedule_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'competition:theory-schedule:view',
    'competition:theory-schedule:manage',
    'competition:theory-schedule:publish',
    'competition:theory-roster:export'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
