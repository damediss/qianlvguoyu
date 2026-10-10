INSERT INTO sys_tenant (
  id, code, name, status, contact_name, contact_phone, remark, created_at, updated_at, deleted
) VALUES (
  1, 'demo', '演示租户', 'enabled', '平台管理员', '13800000000', '一期本地开发演示租户', NOW(), NOW(), 0
);

INSERT INTO sys_org (
  id, tenant_id, parent_id, org_type, code, name, sort_order, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  100, 1, 0, 'company', 'demo-root', '演示公司', 1, 'enabled', NOW(), NOW(), 1, 1, 0
);

INSERT INTO sys_user (
  id, tenant_id, org_id, username, password_hash, display_name, user_type, phone, email, avatar_file_id,
  status, last_login_at, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  1000, 1, 100, 'admin',
  '$2y$10$NeoXS685SYXSeLFA.o1Wj.p6nqLS8RixLRcq7WRh4R1EN4kGNsDDW',
  '平台管理员', 'admin', NULL, NULL, NULL, 'enabled', NULL, NOW(), NOW(), 1, 1, 0
);

INSERT INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (2000, 1, 'PLATFORM_ADMIN', '平台管理员', 'platform', 'enabled', '拥有一期演示环境全部权限', NOW(), NOW(), 1, 1, 0),
  (2001, 1, 'TRAINING_MANAGER', '培训负责人', 'business', 'enabled', '管理课程发布、考试安排、学员学习和成绩', NOW(), NOW(), 1, 1, 0),
  (2002, 1, 'COURSE_STAFF', '课程人员', 'business', 'enabled', '维护课程、章节、题目和试卷', NOW(), NOW(), 1, 1, 0),
  (2003, 1, 'STUDENT', '学员', 'student', 'enabled', '学员学习与考试', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_user_role (
  id, tenant_id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  3000, 1, 1000, 2000, NOW(), NOW(), 1, 1, 0
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4000, 1, 0, 'page', 'dashboard', '管理概览', '/dashboard', 'dashboard/Overview', 'dashboard:view', 'DashboardOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4100, 1, 0, 'directory', 'system', '系统管理', '/system', NULL, 'system:view', 'SettingOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4101, 1, 4100, 'page', 'system_tenant', '租户管理', '/system/tenants', 'system/TenantList', 'tenant:view', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4102, 1, 4100, 'page', 'system_org', '组织管理', '/system/orgs', 'system/OrgTree', 'org:view', NULL, 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4103, 1, 4100, 'page', 'system_user', '用户管理', '/system/users', 'system/UserList', 'user:view', NULL, 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4104, 1, 4100, 'page', 'system_role', '角色权限', '/system/roles', 'system/RoleList', 'role:view', NULL, 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4105, 1, 4100, 'page', 'system_menu', '菜单管理', '/system/menus', 'system/MenuTree', 'menu:view', NULL, 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4106, 1, 4100, 'page', 'system_dict', '字典管理', '/system/dicts', 'system/DictList', 'dict:view', NULL, 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4107, 1, 4100, 'page', 'system_audit_log', '操作日志', '/system/audit-logs', 'system/AuditLogList', 'audit-log:view', NULL, 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4200, 1, 0, 'directory', 'course_center', '课程题库', '/course', NULL, 'course:view', 'BookOutlined', 20, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4201, 1, 4200, 'page', 'course_manage', '课程管理', '/course/courses', 'course/CourseList', 'course:view', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4202, 1, 4200, 'page', 'question_manage', '题库管理', '/course/questions', 'question/QuestionList', 'question:view', NULL, 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4203, 1, 4200, 'page', 'paper_manage', '试卷管理', '/course/exam-papers', 'exam/PaperList', 'exam:view', NULL, 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4300, 1, 0, 'directory', 'training', '培训考试', '/training', NULL, 'exam:view', 'ScheduleOutlined', 30, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4301, 1, 4300, 'page', 'exam_session', '考试安排', '/training/exam-sessions', 'exam/SessionList', 'exam:view', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4400, 1, 0, 'directory', 'workflow', '流程审批', '/workflow', NULL, 'workflow:view', 'BranchesOutlined', 40, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4401, 1, 4400, 'page', 'workflow_tasks', '我的待办', '/workflow/tasks', 'workflow/TaskList', 'workflow:view', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4500, 1, 0, 'directory', 'ai', 'AI 样板', '/ai', NULL, 'ai:course-outline', 'RobotOutlined', 50, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4501, 1, 4500, 'page', 'ai_course_outline', '课程大纲草稿', '/ai/course-outline', 'ai/CourseOutlineDraft', 'ai:course-outline', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5000, 1, 4000, 'dashboard:view', '查看管理概览', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5001, 1, 4101, 'tenant:view', '查看租户', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5002, 1, 4101, 'tenant:create', '新增租户', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5003, 1, 4101, 'tenant:update', '更新租户', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5004, 1, 4101, 'tenant:delete', '删除租户', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5010, 1, 4102, 'org:view', '查看组织', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5011, 1, 4102, 'org:create', '新增组织', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5012, 1, 4102, 'org:update', '更新组织', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5013, 1, 4102, 'org:delete', '删除组织', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5020, 1, 4103, 'user:view', '查看用户', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5021, 1, 4103, 'user:create', '新增用户', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5022, 1, 4103, 'user:update', '更新用户', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5023, 1, 4103, 'user:delete', '删除用户', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5030, 1, 4104, 'role:view', '查看角色', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5031, 1, 4104, 'role:create', '新增角色', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5032, 1, 4104, 'role:update', '更新角色', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5033, 1, 4104, 'role:delete', '删除角色', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5040, 1, 4105, 'menu:view', '查看菜单', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5041, 1, 4105, 'menu:create', '新增菜单', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5042, 1, 4105, 'menu:update', '更新菜单', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5043, 1, 4105, 'menu:delete', '删除菜单', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5050, 1, 4106, 'dict:view', '查看字典', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5051, 1, 4106, 'dict:create', '新增字典', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5052, 1, 4106, 'dict:update', '更新字典', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5055, 1, 4107, 'audit-log:view', '查看操作日志', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5060, 1, 4201, 'course:view', '查看课程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5061, 1, 4201, 'course:create', '新增课程', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5062, 1, 4201, 'course:update', '更新课程', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5063, 1, 4201, 'course:delete', '删除课程', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5070, 1, 4202, 'question:view', '查看题目', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5071, 1, 4202, 'question:create', '新增题目', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5072, 1, 4202, 'question:update', '更新题目', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5073, 1, 4202, 'question:delete', '删除题目', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5080, 1, 4203, 'exam:view', '查看考试', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5081, 1, 4203, 'exam:create', '新增考试', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5082, 1, 4203, 'exam:update', '更新考试', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5090, 1, 4401, 'workflow:view', '查看流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5091, 1, 4401, 'workflow:approve', '流程审批', 'approve', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5100, 1, 4501, 'ai:course-outline', 'AI 课程大纲草稿', 'create', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 600000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE tenant_id = 1 AND deleted = 0;

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 700000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE tenant_id = 1 AND deleted = 0;
