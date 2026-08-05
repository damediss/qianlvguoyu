-- Repair the stage-planning menu for environments where an earlier migration
-- already occupies version 20260801.007.

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);

SET @competition_stage_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_stage' AND deleted = 0
  LIMIT 1
), (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_stage_menu_id, 1, @competition_root_menu_id, 'page',
  'competition_stage', '赛段编排', '/competition/stages',
  'competition/CompetitionEventManagementPage', 'competition:stage:view',
  'PartitionOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET sort_order = CASE code
  WHEN 'competition_notice' THEN 3
  WHEN 'competition_theory_exam' THEN 4
  WHEN 'competition_score_ranking' THEN 5
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND code IN ('competition_notice', 'competition_theory_exam', 'competition_score_ranking')
  AND deleted = 0;

UPDATE sys_permission
SET menu_id = @competition_stage_menu_id,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code IN ('competition:stage:view', 'competition:stage:manage');

SET @next_competition_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_role_menu_id := @next_competition_role_menu_id + 1,
  rp.tenant_id,
  rp.role_id,
  @competition_stage_menu_id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p
  ON p.tenant_id = rp.tenant_id
 AND p.id = rp.permission_id
 AND p.code = 'competition:stage:view'
 AND p.deleted = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.tenant_id = rp.tenant_id
    AND existing.role_id = rp.role_id
    AND existing.menu_id = @competition_stage_menu_id
    AND existing.deleted = 0
);
