-- New menus: Post management + Permission management under system parent (4100)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4108, 1, 4100, 'page', 'system_post', '岗位管理', '/system/posts', 'system/PostList', 'post:view', 'IdcardOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4109, 1, 4100, 'page', 'system_permission', '权限管理', '/system/permissions', 'system/PermissionList', 'role:view', 'LockOutlined', 9, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- New permission codes for post management
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5400, 1, 4108, 'post:view', '查看岗位', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5401, 1, 4108, 'post:create', '新增岗位', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5402, 1, 4108, 'post:update', '更新岗位', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5403, 1, 4108, 'post:delete', '删除岗位', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Bind new menus to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604108, 1, 2000, 4108, NOW(), NOW(), 1, 1, 0),
  (604109, 1, 2000, 4109, NOW(), NOW(), 1, 1, 0);

-- Bind new permissions to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705700, 1, 2000, 5400, NOW(), NOW(), 1, 1, 0),
  (705701, 1, 2000, 5401, NOW(), NOW(), 1, 1, 0),
  (705702, 1, 2000, 5402, NOW(), NOW(), 1, 1, 0),
  (705703, 1, 2000, 5403, NOW(), NOW(), 1, 1, 0);

-- Demo post data for tenant_id=1
INSERT IGNORE INTO sys_post (
  id, tenant_id, code, name, sort_order, status, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8001, 1, 'GM', '总经理', 1, 'enabled', '公司总经理', NOW(), NOW(), 1, 1, 0),
  (8002, 1, 'TECH_DIRECTOR', '技术总监', 2, 'enabled', '技术部门负责人', NOW(), NOW(), 1, 1, 0),
  (8003, 1, 'TRAINING_SPECIALIST', '培训专员', 3, 'enabled', '负责培训业务执行', NOW(), NOW(), 1, 1, 0);
