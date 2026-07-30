INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5120, 1, 5100, 'directory', 'industrial_project', '开发工程', '/industrial/project', NULL, NULL, 'FolderOpenOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5121, 1, 5120, 'page', 'industrial_project_course', '课件列表', '/industrial/project/course', 'industrial/project/CourseView', 'industrial:project-course:view', 'FilePptOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5122, 1, 5120, 'page', 'industrial_project_teach', '教材列表', '/industrial/project/teach', 'industrial/project/TeachView', 'industrial:project-teach:view', 'ReadOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5123, 1, 5120, 'page', 'industrial_project_exam', '题库列表', '/industrial/project/exam', 'industrial/project/ExamView', 'industrial:project-exam:view', 'DatabaseOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5124, 1, 5120, 'page', 'industrial_project_train', '实训中心', '/industrial/project/train', 'industrial/project/TrainView', 'industrial:project-train:view', 'ExperimentOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5125, 1, 5120, 'page', 'industrial_project_criterion', '标准', '/industrial/project/criterion', 'industrial/project/CriterionView', 'industrial:project-criterion:view', 'SafetyOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5760, 1, 5121, 'industrial:project-course:view', '查看课件列表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5761, 1, 5122, 'industrial:project-teach:view', '查看教材列表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5762, 1, 5123, 'industrial:project-exam:view', '查看题库列表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5763, 1, 5124, 'industrial:project-train:view', '查看实训中心', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5764, 1, 5125, 'industrial:project-criterion:view', '查看标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (606120, 1, 2000, 5120, NOW(), NOW(), 1, 1, 0),
  (606121, 1, 2000, 5121, NOW(), NOW(), 1, 1, 0),
  (606122, 1, 2000, 5122, NOW(), NOW(), 1, 1, 0),
  (606123, 1, 2000, 5123, NOW(), NOW(), 1, 1, 0),
  (606124, 1, 2000, 5124, NOW(), NOW(), 1, 1, 0),
  (606125, 1, 2000, 5125, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (706160, 1, 2000, 5760, NOW(), NOW(), 1, 1, 0),
  (706161, 1, 2000, 5761, NOW(), NOW(), 1, 1, 0),
  (706162, 1, 2000, 5762, NOW(), NOW(), 1, 1, 0),
  (706163, 1, 2000, 5763, NOW(), NOW(), 1, 1, 0),
  (706164, 1, 2000, 5764, NOW(), NOW(), 1, 1, 0);
