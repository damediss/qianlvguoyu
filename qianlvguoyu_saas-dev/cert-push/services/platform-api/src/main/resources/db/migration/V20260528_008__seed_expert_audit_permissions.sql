-- Add sys_permission entries for audit task menus (4708, 4709)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5375, 1, 4708, 'task:audit:start:view', '查看初审任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5376, 1, 4709, 'task:audit:end:view', '查看终审任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant permissions to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705580, 1, 2000, 5375, NOW(), NOW(), 1, 1, 0),
  (705581, 1, 2000, 5376, NOW(), NOW(), 1, 1, 0);

-- Grant permissions to EXPERT (role_id=2004)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705582, 1, 2004, 5375, NOW(), NOW(), 1, 1, 0),
  (705583, 1, 2004, 5376, NOW(), NOW(), 1, 1, 0);
