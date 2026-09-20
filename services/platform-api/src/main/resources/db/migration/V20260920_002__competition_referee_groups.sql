-- 赛段实操裁判配置：先维护已确认的裁判组，再将选手绑定到裁判组。
-- 裁判组是赛段配置数据，正式比赛初始化时仍会复制为场次级裁判组。

CREATE TABLE IF NOT EXISTS competition_referee_group (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  group_no INT NOT NULL,
  group_name VARCHAR(128) NOT NULL,
  leader_referee_member_id BIGINT NULL COMMENT '组长对应的赛事裁判成员ID',
  status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/confirmed/closed',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_referee_group_status CHECK (status IN ('draft', 'confirmed', 'closed')),
  UNIQUE KEY uk_comp_referee_group_no (
    tenant_id, stage_id, event_work_type_id, event_work_type_level_id, group_no, deleted
  ),
  KEY idx_comp_referee_group_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛段实操裁判组配置';

CREATE TABLE IF NOT EXISTS competition_referee_group_member (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL COMMENT 'competition_event_member.id',
  member_role VARCHAR(32) NOT NULL DEFAULT 'member' COMMENT 'leader/member',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/removed',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_referee_group_member_role CHECK (member_role IN ('leader', 'member')),
  CONSTRAINT chk_comp_referee_group_member_status CHECK (status IN ('active', 'removed')),
  UNIQUE KEY uk_comp_referee_group_member (
    tenant_id, group_id, referee_member_id, deleted
  ),
  KEY idx_comp_referee_group_member_group (
    tenant_id, group_id, status, deleted
  ),
  KEY idx_comp_referee_group_member_referee (
    tenant_id, referee_member_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛段实操裁判组成员';

ALTER TABLE competition_referee_assignment
  ADD COLUMN referee_group_id BIGINT NULL COMMENT '赛段配置时绑定的裁判组ID' AFTER event_work_type_level_id,
  ADD KEY idx_comp_referee_assignment_group (
    tenant_id, referee_group_id, contestant_member_id, status, deleted
  );
