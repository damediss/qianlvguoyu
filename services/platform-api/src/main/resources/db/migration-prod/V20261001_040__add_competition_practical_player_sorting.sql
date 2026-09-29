CREATE TABLE IF NOT EXISTS competition_practical_sorting_task (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_name VARCHAR(128) NOT NULL,
  group_count INT NOT NULL,
  workstation_count INT NOT NULL,
  players_per_group INT NOT NULL,
  player_count INT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'created',
  selected_profile_ids_json JSON NOT NULL,
  result_snapshot_json JSON NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_competition_sorting_task_stage (tenant_id, stage_id, created_at, deleted),
  KEY idx_competition_sorting_task_event (tenant_id, event_id, deleted)
);

-- 竞赛菜单、权限和角色授权统一落在千铝工投运行租户 10159。
SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);
SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_foundation' AND deleted = 0
  ORDER BY id LIMIT 1
);

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @competition_player_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player'
  ORDER BY id LIMIT 1
);
SET @competition_player_menu_id := COALESCE(
  @competition_player_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_player_menu_id, @competition_tenant_id, @competition_root_menu_id,
  'directory', 'competition_player', '选手管理', '/competition/player-management', NULL,
  'competition:player:view', 'TeamOutlined', 6, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = NULL, permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @competition_player_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @competition_player_info_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_info'
  ORDER BY id LIMIT 1
);
SET @competition_player_info_menu_id := COALESCE(
  @competition_player_info_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);
SET @competition_player_sorting_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_sorting'
  ORDER BY id LIMIT 1
);
SET @competition_player_sorting_menu_id := COALESCE(
  @competition_player_sorting_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@competition_player_info_menu_id, @competition_tenant_id, @competition_player_menu_id,
   'page', 'competition_player_info', '选手信息', '/competition/players',
   'competition/CompetitionPlayerManagementPage', 'competition:player:view',
   'IdcardOutlined', 1, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@competition_player_sorting_menu_id, @competition_tenant_id, @competition_player_menu_id,
   'page', 'competition_player_sorting', '排序分组', '/competition/player-sorting',
   'competition/CompetitionPlayerSortingPage', 'competition:player-sorting:view',
   'AppstoreOutlined', 2, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component),
  permission_code = VALUES(permission_code), icon = VALUES(icon),
  sort_order = VALUES(sort_order), visible = 1, status = 'enabled', deleted = 0,
  updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @player_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player:view'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @player_create_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player:create'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @player_update_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player:update'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @player_delete_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player:delete'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @sorting_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-sorting:view'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @sorting_create_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-sorting:create'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @sorting_randomize_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-sorting:randomize'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @sorting_export_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-sorting:export'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @event_view_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:event:view'
    AND deleted = 0 ORDER BY id LIMIT 1
);
SET @stage_view_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:stage:view'
    AND deleted = 0 ORDER BY id LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@player_view_permission_id, @competition_tenant_id, @competition_player_info_menu_id,
   'competition:player:view', '查看竞赛选手', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@player_create_permission_id, @competition_tenant_id, @competition_player_info_menu_id,
   'competition:player:create', '新增竞赛选手', 'create', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@player_update_permission_id, @competition_tenant_id, @competition_player_info_menu_id,
   'competition:player:update', '编辑竞赛选手', 'update', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@player_delete_permission_id, @competition_tenant_id, @competition_player_info_menu_id,
   'competition:player:delete', '删除竞赛选手', 'delete', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@sorting_view_permission_id, @competition_tenant_id, @competition_player_sorting_menu_id,
   'competition:player-sorting:view', '查看选手排序分组', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@sorting_create_permission_id, @competition_tenant_id, @competition_player_sorting_menu_id,
   'competition:player-sorting:create', '创建选手排序任务', 'create', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@sorting_randomize_permission_id, @competition_tenant_id, @competition_player_sorting_menu_id,
   'competition:player-sorting:randomize', '随机分组排序', 'randomize', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@sorting_export_permission_id, @competition_tenant_id, @competition_player_sorting_menu_id,
   'competition:player-sorting:export', '导出实操出场名单', 'export', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @competition_tenant_id, r.id, target.menu_id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
CROSS JOIN (
  SELECT @competition_player_menu_id AS menu_id
  UNION ALL SELECT @competition_player_info_menu_id
  UNION ALL SELECT @competition_player_sorting_menu_id
) target
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission rp
      JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
      WHERE rp.tenant_id = @competition_tenant_id AND rp.role_id = r.id
        AND rp.deleted = 0 AND p.deleted = 0
        AND p.code IN ('competition:player:view', 'competition:event:view')
    )
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.id IN (
    @player_view_permission_id, @sorting_view_permission_id,
    @event_view_permission_id, @stage_view_permission_id
  )
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission existing_grant
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = existing_grant.tenant_id
       AND existing_permission.id = existing_grant.permission_id
      WHERE existing_grant.tenant_id = @competition_tenant_id
        AND existing_grant.role_id = r.id AND existing_grant.deleted = 0
        AND existing_permission.deleted = 0
        AND existing_permission.code IN ('competition:player:view', 'competition:event:view')
    )
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.id IN (
    @player_create_permission_id, @player_update_permission_id, @player_delete_permission_id
  )
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission existing_grant
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = existing_grant.tenant_id
       AND existing_permission.id = existing_grant.permission_id
      WHERE existing_grant.tenant_id = @competition_tenant_id
        AND existing_grant.role_id = r.id AND existing_grant.deleted = 0
        AND existing_permission.deleted = 0
        AND existing_permission.code = p.code
    )
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.id IN (
    @sorting_create_permission_id, @sorting_randomize_permission_id,
    @sorting_export_permission_id
  )
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission existing_grant
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = existing_grant.tenant_id
       AND existing_permission.id = existing_grant.permission_id
      WHERE existing_grant.tenant_id = @competition_tenant_id
        AND existing_grant.role_id = r.id AND existing_grant.deleted = 0
        AND existing_permission.deleted = 0
        AND existing_permission.code IN (
          'competition:player:create', 'competition:stage:manage'
        )
    )
  );
