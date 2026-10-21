-- 选手抽签排序。取号：测试库 flyway max=20261008.002，生产库 max=20261008.001，共用版本 20261008.003。
-- 第一轮保存随机抽签顺序，第二轮按该顺序逐个写入出场顺序。
-- 空库幂等化（2026-10-21 补，踩坑 #34 同族）：父菜单 competition_player 是存量库 legacy 菜单，
-- 空库不存在时子查询返 NULL 撞 parent_id NOT NULL 断链；父菜单在才挂载（INSERT ... SELECT 守卫）。
-- 存量测试库已应用不会重跑（validate-on-migrate=false）；生产尚未应用到 003，无 checksum 历史。
CREATE TABLE IF NOT EXISTS competition_player_draw_task (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_name VARCHAR(128) NOT NULL,
  player_count INT NOT NULL,
  drawn_count INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL DEFAULT 'created',
  selected_profile_ids_json JSON NOT NULL,
  result_snapshot_json JSON NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_competition_draw_task_stage (tenant_id, stage_id, created_at, deleted),
  KEY idx_competition_draw_task_event (tenant_id, event_id, deleted)
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
SET @competition_player_draw_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_draw'
  ORDER BY id LIMIT 1
);
SET @competition_player_draw_menu_id := COALESCE(
  @competition_player_draw_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @competition_player_draw_menu_id, @competition_tenant_id, @competition_player_menu_id,
  'page', 'competition_player_draw', '抽签排序', '/competition/player-draw',
  'competition/CompetitionPlayerDrawPage', 'competition:player-draw:view',
  'OrderedListOutlined', 3, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
FROM DUAL
WHERE @competition_player_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component),
  permission_code = VALUES(permission_code), icon = VALUES(icon),
  sort_order = VALUES(sort_order), visible = 1, status = 'enabled', deleted = 0,
  updated_at = NOW(), updated_by = VALUES(updated_by);

SET @competition_player_draw_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_draw' AND deleted = 0
  ORDER BY id LIMIT 1
);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @draw_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-draw:view'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @draw_create_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-draw:create'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @draw_execute_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-draw:draw'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @draw_export_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-draw:export'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @draw_delete_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-draw:delete'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@draw_view_permission_id, @competition_tenant_id, @competition_player_draw_menu_id,
   'competition:player-draw:view', '查看选手抽签排序', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@draw_create_permission_id, @competition_tenant_id, @competition_player_draw_menu_id,
   'competition:player-draw:create', '创建选手抽签任务', 'create', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@draw_execute_permission_id, @competition_tenant_id, @competition_player_draw_menu_id,
   'competition:player-draw:draw', '执行选手抽签', 'draw', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@draw_export_permission_id, @competition_tenant_id, @competition_player_draw_menu_id,
   'competition:player-draw:export', '导出抽签出场名单', 'export', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@draw_delete_permission_id, @competition_tenant_id, @competition_player_draw_menu_id,
   'competition:player-draw:delete', '删除未完成抽签任务', 'delete', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @competition_tenant_id, r.id, @competition_player_draw_menu_id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND @competition_player_draw_menu_id IS NOT NULL
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
  AND p.code = 'competition:player-draw:view'
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
  AND p.code IN (
    'competition:player-draw:create', 'competition:player-draw:draw',
    'competition:player-draw:export', 'competition:player-draw:delete'
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
        AND existing_permission.code IN ('competition:player:create', 'competition:stage:manage')
    )
  );
