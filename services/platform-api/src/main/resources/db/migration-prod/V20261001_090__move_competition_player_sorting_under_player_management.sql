SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id
  FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id
  LIMIT 1
), 1);
SET @competition_player_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

UPDATE sys_menu
SET parent_id = @competition_player_menu_id,
    visible = 1,
    sort_order = 2,
    updated_at = NOW(),
    updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id
  AND code = 'competition_player_sorting'
  AND deleted = 0
  AND @competition_player_menu_id IS NOT NULL;
