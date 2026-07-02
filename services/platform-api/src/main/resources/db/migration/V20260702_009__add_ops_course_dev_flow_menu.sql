-- ============================================================
-- 运营流程 → 课程开发流程 菜单树
-- 与业务销售流程（9001）同级，挂在运营流程（9000）下
-- ============================================================

-- 1. 课程开发流程 目录 (parent_id=9000, sort_order=2)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9040, 1, 9000, 'directory', 'ops_course_dev_flow', '课程开发流程', '/ops-flow/course-dev', NULL, 'ops.course_dev_flow:view', 'BookOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 课程开发流程子菜单 (parent_id=9040)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9041, 1, 9040, 'page', 'ops_std_dev_flow',        '标准开发与共建流程',        '/ops-flow/course-dev/standard',      'product/ProductPlaceholder', 'ops.std_dev_flow:view',        'FileTextOutlined',   1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9042, 1, 9040, 'page', 'ops_question_dev_flow',   '题库开发与共建流程',        '/ops-flow/course-dev/question-bank', 'product/ProductPlaceholder', 'ops.question_dev_flow:view',   'DatabaseOutlined',   2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9043, 1, 9040, 'page', 'ops_video_dev_flow',      '视频开发与共建流程',        '/ops-flow/course-dev/video',         'product/ProductPlaceholder', 'ops.video_dev_flow:view',      'PlayCircleOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9044, 1, 9040, 'page', 'ops_simulation_dev_flow', '仿真教材开发与共建流程',    '/ops-flow/course-dev/simulation',    'product/ProductPlaceholder', 'ops.simulation_dev_flow:view', 'ExperimentOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9045, 1, 9040, 'page', 'ops_tech_doc_dev_flow',   '技术文件开发与共建流程',    '/ops-flow/course-dev/tech-doc',      'product/ProductPlaceholder', 'ops.tech_doc_dev_flow:view',   'FolderOpenOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9140, 1, 9040, 'ops.course_dev_flow:view',      '查看课程开发流程',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9141, 1, 9041, 'ops.std_dev_flow:view',         '查看标准开发与共建流程',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9142, 1, 9042, 'ops.question_dev_flow:view',    '查看题库开发与共建流程',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9143, 1, 9043, 'ops.video_dev_flow:view',       '查看视频开发与共建流程',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9144, 1, 9044, 'ops.simulation_dev_flow:view',  '查看仿真教材开发与共建流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9145, 1, 9045, 'ops.tech_doc_dev_flow:view',    '查看技术文件开发与共建流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9040 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9041, 1 UNION ALL SELECT 9042, 1 UNION ALL SELECT 9043, 1 UNION ALL
  SELECT 9044, 1 UNION ALL SELECT 9045, 1
) t;

-- 5. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9140 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9141, 1 UNION ALL SELECT 9142, 1 UNION ALL SELECT 9143, 1 UNION ALL
  SELECT 9144, 1 UNION ALL SELECT 9145, 1
) t;