-- Add audit task menus (审核任务/初审任务/终审任务) under task-management (4704)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4707, 1, 4704, 'directory', 'audit_task', '审核任务', '/task-management/audit-task', NULL, 'task:audit:view', 'SafetyCertificateOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4708, 1, 4707, 'page', 'expert_task_start', '初审任务', '/task-management/audit-task/start', 'expert/TaskStart', 'task:audit:start:view', 'CheckCircleOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4709, 1, 4707, 'page', 'expert_task_end', '终审任务', '/task-management/audit-task/end', 'expert/TaskEnd', 'task:audit:end:view', 'SwapOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant menus to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604922, 1, 2000, 4707, NOW(), NOW(), 1, 1, 0),
  (604923, 1, 2000, 4708, NOW(), NOW(), 1, 1, 0),
  (604924, 1, 2000, 4709, NOW(), NOW(), 1, 1, 0);

-- Grant menus to EXPERT (role_id=2004)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604925, 1, 2004, 4707, NOW(), NOW(), 1, 1, 0),
  (604926, 1, 2004, 4708, NOW(), NOW(), 1, 1, 0),
  (604927, 1, 2004, 4709, NOW(), NOW(), 1, 1, 0);
