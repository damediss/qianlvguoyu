-- Add exam user-auth-course menu and permissions for demo tenant
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4703, 1, 4700, 'page', 'exam_user_auth_course', '专家授权管理', '/question-bank/user-auth-courses', 'exam/UserAuthCourseList', 'exam:user-auth:view', 'SafetyOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5208, 1, 4703, 'exam:user-auth:view', '查看专家授权', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5209, 1, 4703, 'exam:user-auth:create', '新增专家授权', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5210, 1, 4703, 'exam:user-auth:update', '编辑专家授权', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5211, 1, 4703, 'exam:user-auth:delete', '删除专家授权', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604606, 1, 2000, 4703, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705125, 1, 2000, 5208, NOW(), NOW(), 1, 1, 0),
  (705126, 1, 2000, 5209, NOW(), NOW(), 1, 1, 0),
  (705127, 1, 2000, 5210, NOW(), NOW(), 1, 1, 0),
  (705128, 1, 2000, 5211, NOW(), NOW(), 1, 1, 0);
