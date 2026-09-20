-- 裁判小组提升到赛段之上：按租户维护，赛段实操配置只绑定已确定小组。
-- 同时把「裁判管理」拆成「裁判信息」和「裁判小组」两个子菜单。

SET @add_leader_profile_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group ADD COLUMN leader_referee_profile_id BIGINT NULL COMMENT ''组长对应的租户裁判档案ID'' AFTER leader_referee_member_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND column_name = 'leader_referee_profile_id'
);
PREPARE add_leader_profile_stmt FROM @add_leader_profile_sql;
EXECUTE add_leader_profile_stmt;
DEALLOCATE PREPARE add_leader_profile_stmt;

SET @add_member_profile_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group_member ADD COLUMN referee_profile_id BIGINT NULL COMMENT ''租户裁判档案ID'' AFTER group_id',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group_member'
    AND column_name = 'referee_profile_id'
);
PREPARE add_member_profile_stmt FROM @add_member_profile_sql;
EXECUTE add_member_profile_stmt;
DEALLOCATE PREPARE add_member_profile_stmt;

UPDATE competition_referee_group_member m
JOIN competition_event_member em
  ON em.id = m.referee_member_id
 AND em.tenant_id = m.tenant_id
 AND em.deleted = 0
JOIN competition_referee_profile p
  ON p.person_id = em.person_id
 AND p.tenant_id = m.tenant_id
 AND p.deleted = 0
SET m.referee_profile_id = p.id
WHERE m.deleted = 0
  AND m.referee_profile_id IS NULL
  AND m.referee_member_id IS NOT NULL;

UPDATE competition_referee_group g
JOIN competition_event_member em
  ON em.id = g.leader_referee_member_id
 AND em.tenant_id = g.tenant_id
 AND em.deleted = 0
JOIN competition_referee_profile p
  ON p.person_id = em.person_id
 AND p.tenant_id = g.tenant_id
 AND p.deleted = 0
SET g.leader_referee_profile_id = p.id
WHERE g.deleted = 0
  AND g.leader_referee_profile_id IS NULL
  AND g.leader_referee_member_id IS NOT NULL;

UPDATE competition_referee_group g
JOIN competition_referee_group_member m
  ON m.group_id = g.id
 AND m.tenant_id = g.tenant_id
 AND m.deleted = 0
 AND m.member_role = 'leader'
SET g.leader_referee_profile_id = m.referee_profile_id
WHERE g.deleted = 0
  AND g.leader_referee_profile_id IS NULL
  AND m.referee_profile_id IS NOT NULL;

UPDATE competition_referee_group g
JOIN (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY group_no, id) AS new_no
    FROM competition_referee_group
   WHERE deleted = 0
) numbered ON numbered.id = g.id
SET g.group_no = numbered.new_no
WHERE g.deleted = 0;

SET @nullable_scope_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_group
       MODIFY COLUMN event_id BIGINT NULL COMMENT ''历史赛事ID，新小组不再绑定赛段'',
       MODIFY COLUMN stage_id BIGINT NULL COMMENT ''历史赛段ID，新小组层级高于赛段'',
       MODIFY COLUMN event_work_type_id BIGINT NULL,
       MODIFY COLUMN event_work_type_level_id BIGINT NULL,
       MODIFY COLUMN leader_referee_member_id BIGINT NULL COMMENT ''历史赛事成员ID，新小组以档案ID为准''')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND column_name = 'stage_id'
    AND is_nullable = 'NO'
);
PREPARE nullable_scope_stmt FROM @nullable_scope_sql;
EXECUTE nullable_scope_stmt;
DEALLOCATE PREPARE nullable_scope_stmt;

SET @nullable_member_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_group_member
       MODIFY COLUMN referee_member_id BIGINT NULL COMMENT ''历史赛事成员ID，新成员以档案ID为准''')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group_member'
    AND column_name = 'referee_member_id'
    AND is_nullable = 'NO'
);
PREPARE nullable_member_stmt FROM @nullable_member_sql;
EXECUTE nullable_member_stmt;
DEALLOCATE PREPARE nullable_member_stmt;

UPDATE competition_referee_group
   SET event_id = NULL,
       stage_id = NULL,
       event_work_type_id = NULL,
       event_work_type_level_id = NULL
 WHERE deleted = 0;

SET @drop_group_uk_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_group DROP INDEX uk_comp_referee_group_no')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND index_name = 'uk_comp_referee_group_no'
);
PREPARE drop_group_uk_stmt FROM @drop_group_uk_sql;
EXECUTE drop_group_uk_stmt;
DEALLOCATE PREPARE drop_group_uk_stmt;

SET @add_group_uk_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group ADD UNIQUE KEY uk_comp_referee_group_no (tenant_id, group_no, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND index_name = 'uk_comp_referee_group_no'
);
PREPARE add_group_uk_stmt FROM @add_group_uk_sql;
EXECUTE add_group_uk_stmt;
DEALLOCATE PREPARE add_group_uk_stmt;

SET @drop_group_scope_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_group DROP INDEX idx_comp_referee_group_scope')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND index_name = 'idx_comp_referee_group_scope'
);
PREPARE drop_group_scope_stmt FROM @drop_group_scope_sql;
EXECUTE drop_group_scope_stmt;
DEALLOCATE PREPARE drop_group_scope_stmt;

SET @add_group_scope_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group ADD KEY idx_comp_referee_group_scope (tenant_id, status, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group'
    AND index_name = 'idx_comp_referee_group_scope'
);
PREPARE add_group_scope_stmt FROM @add_group_scope_sql;
EXECUTE add_group_scope_stmt;
DEALLOCATE PREPARE add_group_scope_stmt;

SET @drop_member_uk_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT 1',
    'ALTER TABLE competition_referee_group_member DROP INDEX uk_comp_referee_group_member')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group_member'
    AND index_name = 'uk_comp_referee_group_member'
);
PREPARE drop_member_uk_stmt FROM @drop_member_uk_sql;
EXECUTE drop_member_uk_stmt;
DEALLOCATE PREPARE drop_member_uk_stmt;

SET @add_member_uk_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group_member ADD UNIQUE KEY uk_comp_referee_group_member (tenant_id, group_id, referee_profile_id, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group_member'
    AND index_name = 'uk_comp_referee_group_member'
);
PREPARE add_member_uk_stmt FROM @add_member_uk_sql;
EXECUTE add_member_uk_stmt;
DEALLOCATE PREPARE add_member_uk_stmt;

SET @add_member_profile_idx_sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_referee_group_member ADD KEY idx_comp_referee_group_member_profile (tenant_id, referee_profile_id, status, deleted)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_referee_group_member'
    AND index_name = 'idx_comp_referee_group_member_profile'
);
PREPARE add_member_profile_idx_stmt FROM @add_member_profile_idx_sql;
EXECUTE add_member_profile_idx_stmt;
DEALLOCATE PREPARE add_member_profile_idx_stmt;

SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @referee_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_referee' AND deleted = 0
  LIMIT 1
);

UPDATE sys_menu
SET menu_type = 'directory',
    name = '裁判管理',
    path = '/competition/referees',
    component = NULL,
    permission_code = 'competition:referee:view',
    updated_at = NOW(),
    updated_by = @seed_operator_id
WHERE id = @referee_menu_id
  AND tenant_id = @competition_tenant_id
  AND deleted = 0;

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @referee_info_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_referee_info' AND deleted = 0
  LIMIT 1
);
SET @referee_info_menu_id := COALESCE(
  @referee_info_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);
SET @referee_group_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_referee_group' AND deleted = 0
  LIMIT 1
);
SET @referee_group_menu_id := COALESCE(
  @referee_group_menu_id,
  (@next_menu_id := @next_menu_id + 1)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @referee_info_menu_id, @competition_tenant_id, @referee_menu_id, 'page', 'competition_referee_info',
  '裁判信息', '/competition/referees/info', 'competition/CompetitionRefereeManagementPage',
  'competition:referee:view', 'IdcardOutlined', 1, 1, 'enabled',
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
  sort_order = VALUES(sort_order),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = VALUES(updated_by);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @referee_group_menu_id, @competition_tenant_id, @referee_menu_id, 'page', 'competition_referee_group',
  '裁判小组', '/competition/referees/groups', 'competition/CompetitionRefereeGroupPage',
  'competition:referee-group:view', 'TeamOutlined', 2, 1, 'enabled',
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
  sort_order = VALUES(sort_order),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = VALUES(updated_by);

UPDATE sys_permission
SET menu_id = @referee_info_menu_id,
    updated_at = NOW(),
    updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id
  AND deleted = 0
  AND code IN (
    'competition:referee:view',
    'competition:referee:create',
    'competition:referee:update',
    'competition:referee:delete'
  );

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @group_view_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee-group:view' AND deleted = 0
  LIMIT 1
);
SET @group_view_permission_id := COALESCE(
  @group_view_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);
SET @group_manage_permission_id := (
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:referee-group:manage' AND deleted = 0
  LIMIT 1
);
SET @group_manage_permission_id := COALESCE(
  @group_manage_permission_id,
  (@next_permission_id := @next_permission_id + 1)
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@group_view_permission_id, @competition_tenant_id, @referee_group_menu_id,
    'competition:referee-group:view', '查看竞赛裁判小组', 'view', 'enabled',
    NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@group_manage_permission_id, @competition_tenant_id, @referee_group_menu_id,
    'competition:referee-group:manage', '管理竞赛裁判小组', 'manage', 'enabled',
    NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_menu_id := @next_role_menu_id + 1, @competition_tenant_id, r.id,
       @referee_info_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = @competition_tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = @referee_menu_id
      AND rm.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_menu_id := @next_role_menu_id + 1, @competition_tenant_id, r.id,
       @referee_group_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = @competition_tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = @referee_menu_id
      AND rm.deleted = 0
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @competition_tenant_id, r.id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.id IN (@group_view_permission_id, @group_manage_permission_id)
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
        AND existing_permission.code IN (
          'competition:referee:view',
          'competition:referee:create',
          'competition:event:view'
        )
    )
  );
