-- 选手管理新增排序分类。取号：测试库 max=20261009.002，生产库 max=20261009.001，共用版本 20261009.003。
CREATE TABLE IF NOT EXISTS competition_player_classify_task (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_name VARCHAR(128) NOT NULL,
  type_count INT NOT NULL,
  player_count INT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'classified',
  type_names_json JSON NOT NULL,
  selected_profile_ids_json JSON NOT NULL,
  result_snapshot_json JSON NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_competition_classify_task_stage (tenant_id, stage_id, created_at, deleted),
  KEY idx_competition_classify_task_event (tenant_id, event_id, deleted)
);

SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);
SET @competition_player_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player' AND deleted = 0
  ORDER BY id LIMIT 1
);

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @competition_player_classify_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_classify'
  ORDER BY id LIMIT 1
);
SET @competition_player_classify_menu_id := COALESCE(
  @competition_player_classify_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_player_classify_menu_id, @competition_tenant_id, @competition_player_menu_id,
  'page', 'competition_player_classify', '排序分类', '/competition/player-classify',
  'competition/CompetitionPlayerClassifyPage', 'competition:player-classify:view',
  'AppstoreOutlined', 4, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component),
  permission_code = VALUES(permission_code), icon = VALUES(icon),
  sort_order = VALUES(sort_order), visible = 1, status = 'enabled', deleted = 0,
  updated_at = NOW(), updated_by = VALUES(updated_by);

SET @competition_player_classify_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_classify' AND deleted = 0
  ORDER BY id LIMIT 1
);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @classify_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-classify:view'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @classify_create_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-classify:create'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @classify_delete_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-classify:delete'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@classify_view_permission_id, @competition_tenant_id, @competition_player_classify_menu_id,
   'competition:player-classify:view', '查看排序分类', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@classify_create_permission_id, @competition_tenant_id, @competition_player_classify_menu_id,
   'competition:player-classify:create', '执行排序分类', 'create', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@classify_delete_permission_id, @competition_tenant_id, @competition_player_classify_menu_id,
   'competition:player-classify:delete', '删除排序分类', 'delete', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @competition_tenant_id, r.id, @competition_player_classify_menu_id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND @competition_player_classify_menu_id IS NOT NULL
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
  AND p.code = 'competition:player-classify:view'
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
  AND p.code IN ('competition:player-classify:create', 'competition:player-classify:delete')
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
        AND existing_permission.code IN ('competition:player:create', 'competition:stage:manage')
    )
  );
