-- Add exam work-type menu and permissions for demo tenant
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4700, 1, 0, 'directory', 'question_bank', '题库管理', '/question-bank', NULL, 'exam:work-type:view', 'BookOutlined', 22, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4701, 1, 4700, 'page', 'exam_work_type', '工种管理', '/question-bank/work-types', 'exam/WorkTypeList', 'exam:work-type:view', 'ExperimentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5200, 1, 4701, 'exam:work-type:view', '查看工种', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5201, 1, 4701, 'exam:work-type:create', '新增工种', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5202, 1, 4701, 'exam:work-type:update', '编辑工种', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5203, 1, 4701, 'exam:work-type:delete', '删除工种', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604603, 1, 2000, 4700, NOW(), NOW(), 1, 1, 0),
  (604604, 1, 2000, 4701, NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705117, 1, 2000, 5200, NOW(), NOW(), 1, 1, 0),
  (705118, 1, 2000, 5201, NOW(), NOW(), 1, 1, 0),
  (705119, 1, 2000, 5202, NOW(), NOW(), 1, 1, 0),
  (705120, 1, 2000, 5203, NOW(), NOW(), 1, 1, 0);