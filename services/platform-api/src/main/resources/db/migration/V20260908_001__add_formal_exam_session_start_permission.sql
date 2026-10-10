SET @competition_formal_exam_menu_id := (
  SELECT COALESCE(
    (
      SELECT id FROM sys_menu
      WHERE code = 'competition_formal_exam' AND deleted = 0
      ORDER BY id LIMIT 1
    ),
    (
      SELECT id FROM sys_menu
      WHERE code = 'competition_event' AND deleted = 0
      ORDER BY id LIMIT 1
    )
  )
);

SET @next_competition_formal_exam_start_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_permission
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_competition_formal_exam_start_permission_id := @next_competition_formal_exam_start_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_formal_exam_menu_id), 1),
   @competition_formal_exam_menu_id, 'competition:exam-session:start',
   '开始正式比赛', 'start', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_formal_exam_start_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_start_role_permission_id :=
    @next_competition_formal_exam_start_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.code = 'competition:exam-session:start'
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_start_role_permission_id :=
    @next_competition_formal_exam_start_role_permission_id + 1,
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
  AND target_permission.code = 'competition:exam-session:start'
WHERE base_role.deleted = 0
  AND base_permission.code IN (
    'competition:exam-session:prepare',
    'competition:theory-paper:manage',
    'competition:theory-paper:confirm',
    'competition:stage-paper-assignment:manage',
    'competition:stage-paper-assignment:confirm'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing_role_permission
    WHERE existing_role_permission.tenant_id = target_permission.tenant_id
      AND existing_role_permission.role_id = base_role.role_id
      AND existing_role_permission.permission_id = target_permission.id
      AND existing_role_permission.deleted = 0
  );
