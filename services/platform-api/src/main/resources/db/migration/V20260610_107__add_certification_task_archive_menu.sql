INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  6350, 1, 6300, 'page', 'certification_task_archive', '任务档案',
  '/certification/task-archive', 'certification/TaskArchive',
  'certification.archive.task.view', 'FileTextOutlined', 6, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
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
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806350, 1, 2000, 6350, NOW(), NOW(), 1, 1, 0),
  (999350, 1, 2604, 6350, NOW(), NOW(), 1, 1, 0);
