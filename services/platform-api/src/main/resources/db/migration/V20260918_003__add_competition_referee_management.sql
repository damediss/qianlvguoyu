-- 竞赛裁判管理：扩展既有裁判角色档案，并补齐裁判管理页面及赛段绑定权限。
-- 裁判身份证号、手机号继续复用 competition_player_identity，避免重复存储敏感身份数据。

SET @referee_name_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD COLUMN referee_name VARCHAR(128) NULL COMMENT ''裁判姓名'' AFTER member_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'referee_name'
);
PREPARE referee_name_stmt FROM @referee_name_sql;
EXECUTE referee_name_stmt;
DEALLOCATE PREPARE referee_name_stmt;

SET @referee_work_type_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD COLUMN event_work_type_id BIGINT NULL COMMENT ''关联赛事工种'' AFTER referee_name',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'event_work_type_id'
);
PREPARE referee_work_type_stmt FROM @referee_work_type_sql;
EXECUTE referee_work_type_stmt;
DEALLOCATE PREPARE referee_work_type_stmt;

SET @referee_level_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD COLUMN event_work_type_level_id BIGINT NULL COMMENT ''关联赛事工种等级'' AFTER event_work_type_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND column_name = 'event_work_type_level_id'
);
PREPARE referee_level_stmt FROM @referee_level_sql;
EXECUTE referee_level_stmt;
DEALLOCATE PREPARE referee_level_stmt;

SET @referee_scope_index_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_profile ADD KEY idx_competition_referee_profile_scope (tenant_id, event_work_type_id, event_work_type_level_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_profile'
    AND index_name = 'idx_competition_referee_profile_scope'
);
PREPARE referee_scope_index_stmt FROM @referee_scope_index_sql;
EXECUTE referee_scope_index_stmt;
DEALLOCATE PREPARE referee_scope_index_stmt;

-- 运行环境的竞赛菜单、权限和角色授权统一落在千铝工投租户 10159。
SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @competition_root_menu_id := COALESCE(
  @competition_root_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_root_menu_id, @competition_tenant_id, 0, 'directory', 'competition_foundation', '竞赛管理',
  '/competition', NULL, 'competition:event:view', 'TrophyOutlined', 25, 1, 'enabled',
  NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  permission_code = VALUES(permission_code),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = VALUES(updated_by);

SET @referee_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_referee' AND deleted = 0
  LIMIT 1
);
SET @referee_menu_id := COALESCE(
  @referee_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @referee_menu_id, @competition_tenant_id, @competition_root_menu_id, 'page', 'competition_referee',
  '裁判管理', '/competition/referees', 'competition/CompetitionRefereeManagementPage',
  'competition:referee:view', 'SafetyCertificateOutlined', 6, 1, 'enabled',
  NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = VALUES(updated_by);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @referee_view_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee:view' AND deleted = 0
  LIMIT 1
);
SET @referee_view_permission_id := COALESCE(
  @referee_view_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @referee_create_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee:create' AND deleted = 0
  LIMIT 1
);
SET @referee_create_permission_id := COALESCE(
  @referee_create_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @referee_update_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee:update' AND deleted = 0
  LIMIT 1
);
SET @referee_update_permission_id := COALESCE(
  @referee_update_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @referee_delete_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee:delete' AND deleted = 0
  LIMIT 1
);
SET @referee_delete_permission_id := COALESCE(
  @referee_delete_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @referee_assignment_view_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee-assignment:view' AND deleted = 0
  LIMIT 1
);
SET @referee_assignment_view_permission_id := COALESCE(
  @referee_assignment_view_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @referee_assignment_manage_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee-assignment:manage' AND deleted = 0
  LIMIT 1
);
SET @referee_assignment_manage_permission_id := COALESCE(
  @referee_assignment_manage_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@referee_view_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee:view', '查看竞赛裁判', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@referee_create_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee:create', '新增竞赛裁判', 'create', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@referee_update_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee:update', '编辑竞赛裁判', 'update', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@referee_delete_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee:delete', '删除竞赛裁判', 'delete', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@referee_assignment_view_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee-assignment:view', '查看赛段裁判绑定', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@referee_assignment_manage_permission_id, @competition_tenant_id, @referee_menu_id,
    'competition:referee-assignment:manage', '管理赛段裁判绑定', 'manage', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_menu_id := @next_role_menu_id + 1, @competition_tenant_id, r.id,
       @competition_root_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND (r.id = 2000 OR EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
    WHERE rp.tenant_id = @competition_tenant_id AND rp.role_id = r.id AND rp.deleted = 0
      AND p.code = 'competition:event:view' AND p.deleted = 0
  ));

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_menu_id := @next_role_menu_id + 1, @competition_tenant_id, r.id,
       @referee_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND (r.id = 2000 OR EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
    WHERE rp.tenant_id = @competition_tenant_id AND rp.role_id = r.id AND rp.deleted = 0
      AND p.code = 'competition:event:view' AND p.deleted = 0
  ));

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @competition_tenant_id, r.id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.id IN (
    @referee_view_permission_id,
    @referee_create_permission_id,
    @referee_update_permission_id,
    @referee_delete_permission_id,
    @referee_assignment_view_permission_id,
    @referee_assignment_manage_permission_id
  )
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = rp.tenant_id
       AND existing_permission.id = rp.permission_id
      WHERE rp.tenant_id = @competition_tenant_id AND rp.role_id = r.id AND rp.deleted = 0
        AND existing_permission.deleted = 0
        AND (
          existing_permission.code = 'competition:event:view'
          OR existing_permission.code = 'competition:stage:manage'
        )
    )
  );
