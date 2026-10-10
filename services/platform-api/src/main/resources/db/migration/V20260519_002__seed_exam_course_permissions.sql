-- Add exam course menu and permissions for demo tenant
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4702, 1, 4700, 'page', 'exam_course', '科目管理', '/question-bank/courses', 'exam/CourseList', 'exam:course:view', 'ReadOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5204, 1, 4702, 'exam:course:view', '查看科目', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5205, 1, 4702, 'exam:course:create', '新增科目', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5206, 1, 4702, 'exam:course:update', '编辑科目', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5207, 1, 4702, 'exam:course:delete', '删除科目', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604605, 1, 2000, 4702, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705121, 1, 2000, 5204, NOW(), NOW(), 1, 1, 0),
  (705122, 1, 2000, 5205, NOW(), NOW(), 1, 1, 0),
  (705123, 1, 2000, 5206, NOW(), NOW(), 1, 1, 0),
  (705124, 1, 2000, 5207, NOW(), NOW(), 1, 1, 0);
