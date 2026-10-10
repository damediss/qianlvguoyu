-- Add EXPERT role
INSERT IGNORE INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (2004, 1, 'EXPERT', '专家', 'business', 'enabled', '题库审核专家，负责任务初审/终审', NOW(), NOW(), 1, 1, 0);

-- Add permissions under menu 4706 (细目表)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5370, 1, 4706, 'task:ximu:view', '查看细目表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5371, 1, 4706, 'task:ximu:import', '导入细目表', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5372, 1, 4706, 'task:ximu:assign-expert', '指派专家', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5373, 1, 4706, 'task:ximu:export', '导出细目表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5374, 1, 4706, 'task:ximu:delete', '删除细目表', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant menu 4706 to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604921, 1, 2000, 4706, NOW(), NOW(), 1, 1, 0);

-- Grant all 5 ximu permissions to PLATFORM_ADMIN
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705575, 1, 2000, 5370, NOW(), NOW(), 1, 1, 0),
  (705576, 1, 2000, 5371, NOW(), NOW(), 1, 1, 0),
  (705577, 1, 2000, 5372, NOW(), NOW(), 1, 1, 0),
  (705578, 1, 2000, 5373, NOW(), NOW(), 1, 1, 0),
  (705579, 1, 2000, 5374, NOW(), NOW(), 1, 1, 0);
