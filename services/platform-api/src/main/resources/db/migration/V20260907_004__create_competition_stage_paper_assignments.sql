-- 竞赛赛段选手试卷配置：独立于场次/考场排考，承载先分卷、后排考的阶段配置。

CREATE TABLE competition_stage_paper_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NULL,
  allocation_source VARCHAR(16) NOT NULL DEFAULT 'manual',
  status VARCHAR(16) NOT NULL DEFAULT 'draft',
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_paper_assignment_source CHECK (
    allocation_source IN ('random', 'manual')
  ),
  CONSTRAINT chk_comp_stage_paper_assignment_status CHECK (
    status IN ('draft', 'confirmed')
  ),
  UNIQUE KEY uk_comp_stage_paper_assignment_contestant (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, contestant_profile_id, deleted
  ),
  KEY idx_comp_stage_paper_assignment_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, paper_snapshot_id, status, deleted
  ),
  KEY idx_comp_stage_paper_assignment_member (
    tenant_id, contestant_member_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛段选手试卷配置';

-- 沿用现有理论考试菜单，不新增菜单；权限仍通过菜单→权限码→角色授权链生效。
SET @competition_paper_assignment_menu_id := COALESCE(
  (
    SELECT id FROM sys_menu
    WHERE code = 'competition_theory_exam' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  (
    SELECT id FROM sys_menu
    WHERE code = 'competition_event' AND deleted = 0
    ORDER BY id LIMIT 1
  )
);

SET @next_competition_paper_assignment_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_permission
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_competition_paper_assignment_permission_id := @next_competition_paper_assignment_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_paper_assignment_menu_id), 1),
   @competition_paper_assignment_menu_id, 'competition:stage-paper-assignment:view',
   '查看赛段选手试卷配置', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_paper_assignment_permission_id := @next_competition_paper_assignment_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_paper_assignment_menu_id), 1),
   @competition_paper_assignment_menu_id, 'competition:stage-paper-assignment:manage',
   '管理赛段选手试卷配置', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_paper_assignment_permission_id := @next_competition_paper_assignment_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_paper_assignment_menu_id), 1),
   @competition_paper_assignment_menu_id, 'competition:stage-paper-assignment:confirm',
   '确认赛段选手试卷配置', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_paper_assignment_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

-- 平台管理员始终具备完整配置能力。
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_paper_assignment_role_permission_id :=
    @next_competition_paper_assignment_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.code IN (
  'competition:stage-paper-assignment:view',
  'competition:stage-paper-assignment:manage',
  'competition:stage-paper-assignment:confirm'
)
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

-- 复用已有理论试卷角色的权限边界，避免产生新的菜单角色模型。
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_paper_assignment_role_permission_id :=
    @next_competition_paper_assignment_role_permission_id + 1,
  target_permission.tenant_id, base_role.role_id, target_permission.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission base_role
JOIN sys_permission base_permission
  ON base_permission.id = base_role.permission_id
  AND base_permission.tenant_id = base_role.tenant_id
  AND base_permission.deleted = 0
JOIN sys_permission target_permission
  ON target_permission.tenant_id = base_permission.tenant_id
  AND target_permission.deleted = 0
  AND target_permission.code = CASE
    WHEN base_permission.code = 'competition:theory-paper:view'
      THEN 'competition:stage-paper-assignment:view'
    WHEN base_permission.code = 'competition:theory-paper:manage'
      THEN 'competition:stage-paper-assignment:manage'
    ELSE 'competition:stage-paper-assignment:confirm'
  END
WHERE base_role.deleted = 0
  AND base_permission.code IN (
    'competition:theory-paper:view',
    'competition:theory-paper:manage',
    'competition:theory-paper:confirm'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing_role_permission
    WHERE existing_role_permission.tenant_id = target_permission.tenant_id
      AND existing_role_permission.role_id = base_role.role_id
      AND existing_role_permission.permission_id = target_permission.id
      AND existing_role_permission.deleted = 0
  );
