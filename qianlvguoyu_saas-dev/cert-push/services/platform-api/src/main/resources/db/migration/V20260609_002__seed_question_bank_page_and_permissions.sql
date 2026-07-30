-- 试题管理菜单 + 知识点权限 + 角色关联
-- 挂载在 "提升考试管理"(id=2064170197574156289) 目录下

-- 1. 菜单
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(6400, 1, 2064170197574156289, 'page', 'exam_question_bank', '试题管理',
 '/question-bank/questions', 'exam/QuestionBank', 'exam:question:view',
 'FormOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 权限 (12条)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(7100, 1, 6400, 'exam:knowledge:view',   '查看知识点',   'view',   'enabled', NOW(), NOW(), 1, 1, 0),
(7101, 1, 6400, 'exam:knowledge:create', '创建知识点',   'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(7102, 1, 6400, 'exam:knowledge:update', '修改知识点',   'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(7103, 1, 6400, 'exam:knowledge:delete', '删除知识点',   'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
(7104, 1, 6400, 'exam:knowledge:import', '导入知识点',   'import', 'enabled', NOW(), NOW(), 1, 1, 0),
(7105, 1, 6400, 'exam:question:view',    '查看试题',     'view',   'enabled', NOW(), NOW(), 1, 1, 0),
(7106, 1, 6400, 'exam:question:create',  '创建试题',     'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(7107, 1, 6400, 'exam:question:update',  '修改试题',     'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(7108, 1, 6400, 'exam:question:delete',  '删除试题',     'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
(7109, 1, 6400, 'exam:question:import',  '导入试题',     'import', 'enabled', NOW(), NOW(), 1, 1, 0),
(7110, 1, 6400, 'exam:question:export',  '导出试题',     'export', 'enabled', NOW(), NOW(), 1, 1, 0),
(7111, 1, 6400, 'exam:knowledge:export', '导出知识点',   'export', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 角色关联 PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(920000, 1, 2000, 6400, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(920100, 1, 2000, 7100, NOW(), NOW(), 1, 1, 0),
(920101, 1, 2000, 7101, NOW(), NOW(), 1, 1, 0),
(920102, 1, 2000, 7102, NOW(), NOW(), 1, 1, 0),
(920103, 1, 2000, 7103, NOW(), NOW(), 1, 1, 0),
(920104, 1, 2000, 7104, NOW(), NOW(), 1, 1, 0),
(920105, 1, 2000, 7105, NOW(), NOW(), 1, 1, 0),
(920106, 1, 2000, 7106, NOW(), NOW(), 1, 1, 0),
(920107, 1, 2000, 7107, NOW(), NOW(), 1, 1, 0),
(920108, 1, 2000, 7108, NOW(), NOW(), 1, 1, 0),
(920109, 1, 2000, 7109, NOW(), NOW(), 1, 1, 0),
(920110, 1, 2000, 7110, NOW(), NOW(), 1, 1, 0),
(920111, 1, 2000, 7111, NOW(), NOW(), 1, 1, 0);
