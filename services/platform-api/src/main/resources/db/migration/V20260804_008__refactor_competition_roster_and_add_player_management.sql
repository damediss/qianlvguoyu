-- 选择赛事工种后导入；批次仅作为预检/确认的后台技术载体。
ALTER TABLE competition_roster_import
  ADD COLUMN event_work_type_id BIGINT NULL COMMENT '本次导入选择的赛事工种' AFTER org_id,
  ADD KEY idx_competition_roster_import_event_work_type (
    tenant_id, event_id, event_work_type_id, deleted
  );

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);

SET @competition_player_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_player' AND deleted = 0
  LIMIT 1
), (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_player_menu_id, 1, @competition_root_menu_id, 'page', 'competition_player',
  '选手管理', '/competition/players', 'competition/CompetitionEventManagementPage',
  'competition:player:view', 'TeamOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), name = VALUES(name), path = VALUES(path),
  component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = VALUES(visible),
  status = VALUES(status), deleted = 0, updated_at = NOW(), updated_by = 1;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9989, 1, @competition_player_menu_id, 'competition:player:view', '查看竞赛选手', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9990, 1, @competition_player_menu_id, 'competition:player:create', '新增竞赛选手', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9991, 1, @competition_player_menu_id, 'competition:player:update', '编辑竞赛选手', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9992, 1, @competition_player_menu_id, 'competition:player:delete', '删除竞赛选手', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = 1;

SET @next_competition_player_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_player_role_permission_id := @next_competition_player_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1 AND p.deleted = 0
  AND p.code IN (
    'competition:player:view', 'competition:player:create',
    'competition:player:update', 'competition:player:delete'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id AND rp.role_id = 2000
      AND rp.permission_id = p.id AND rp.deleted = 0
  );

SET @next_competition_player_role_menu_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_menu
);
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_player_role_menu_id := @next_competition_player_role_menu_id + 1,
  rp.tenant_id, rp.role_id, @competition_player_menu_id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
  AND p.tenant_id = rp.tenant_id AND p.code = 'competition:player:view' AND p.deleted = 0
WHERE rp.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = rp.tenant_id AND existing.role_id = rp.role_id
      AND existing.menu_id = @competition_player_menu_id AND existing.deleted = 0
  );
