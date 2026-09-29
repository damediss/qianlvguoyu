-- Add a separate delete permission for unfinished practical sorting tasks.
SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id
  FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id
  LIMIT 1
), 1);
SET @sorting_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_sorting'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @sorting_delete_permission_id := COALESCE((
  SELECT id
  FROM sys_permission
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition:player-sorting:delete'
  ORDER BY id
  LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT @sorting_delete_permission_id, @competition_tenant_id, @sorting_menu_id,
       'competition:player-sorting:delete', '删除未执行排序任务', 'delete', 'enabled',
       NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
WHERE @sorting_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @sorting_delete_permission_id := (
  SELECT id
  FROM sys_permission
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition:player-sorting:delete'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);
SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = @competition_tenant_id
 AND p.id = @sorting_delete_permission_id
 AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id
  AND r.deleted = 0
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1
      FROM sys_role_permission existing_grant
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = existing_grant.tenant_id
       AND existing_permission.id = existing_grant.permission_id
      WHERE existing_grant.tenant_id = @competition_tenant_id
        AND existing_grant.role_id = r.id
        AND existing_grant.deleted = 0
        AND existing_permission.deleted = 0
        AND existing_permission.code IN (
          'competition:player:create', 'competition:stage:manage'
        )
    )
  );
