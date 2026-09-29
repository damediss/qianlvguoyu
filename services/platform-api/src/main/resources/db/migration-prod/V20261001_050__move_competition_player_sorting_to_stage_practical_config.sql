-- The sorting workspace is launched from the stage node context menu under
-- Practical configuration. Keep that relationship in the menu tree while hiding
-- the context-only entries from the standalone sidebar.
SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id
  FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id
  LIMIT 1
), 1);
SET @competition_stage_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_stage'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @competition_practical_config_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_stage_practical_config'
  ORDER BY id
  LIMIT 1
);
SET @competition_practical_config_menu_id := COALESCE(
  @competition_practical_config_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_practical_config_menu_id, @competition_tenant_id, @competition_stage_menu_id,
  'directory', 'competition_stage_practical_config', '实操配置',
  '/competition/stages/practical-config', NULL, 'competition:stage:view',
  'ToolOutlined', 3, 0, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = NULL, permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 0,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

UPDATE sys_menu
SET parent_id = @competition_practical_config_menu_id,
    visible = 0,
    updated_at = NOW(),
    updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id
  AND code = 'competition_player_sorting'
  AND deleted = 0
  AND @competition_stage_menu_id IS NOT NULL;
