-- Add training department menu items for demo tenant
-- Parent: training directory (id=4300)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4801, 1, 4300, 'page', 'train_needs', '培训需求', '/training/needs', 'training/TrainingNeeds', 'train:need:view', 'ExperimentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4802, 1, 4300, 'page', 'train_solutions', '培训方案', '/training/solutions', 'training/TrainingSolutions', 'train:solution:view', 'BookOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4803, 1, 4300, 'page', 'train_requests', '培训请求', '/training/requests', 'training/TrainingRequests', 'train:request:view', 'ApiOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4804, 1, 4300, 'page', 'train_declares', '培训申报', '/training/declares', 'training/TrainingDeclares', 'train:declare:view', 'AuditOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4805, 1, 4300, 'page', 'train_people', '培训人员', '/training/people', 'training/TrainingPeople', 'train:people:view', 'UserOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4806, 1, 4300, 'page', 'train_classes', '班级管理', '/training/classes', 'training/TrainingClasses', 'train:class:view', 'DatabaseOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4807, 1, 4300, 'page', 'train_courses', '课程管理', '/training/courses', 'training/TrainingCourses', 'train:course:view', 'BookOutlined', 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4808, 1, 4300, 'page', 'train_resources', '资源管理', '/training/resources', 'training/TrainingResources', 'train:resource:view', 'SettingOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4809, 1, 4300, 'page', 'train_tasks', '任务管理', '/training/tasks', 'training/TrainingTasks', 'train:task:view', 'BranchesOutlined', 9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4810, 1, 4300, 'page', 'train_exams', '考试管理', '/training/exams', 'training/TrainingExams', 'train:exam:view', 'SearchOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4811, 1, 4300, 'page', 'train_budgets', '预算管理', '/training/budgets', 'training/TrainingBudgets', 'train:budget:view', 'DollarOutlined', 11, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4812, 1, 4300, 'page', 'train_certificates', '认定管理', '/training/certificates', 'training/TrainingCertificates', 'train:certificate:view', 'SafetyCertificateOutlined', 12, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4813, 1, 4300, 'page', 'train_talent_needs', '人才需求', '/training/talent-needs', 'training/TrainingTalentNeeds', 'train:talent:view', 'TeamOutlined', 13, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4814, 1, 4300, 'page', 'train_teachers', '教师库', '/training/teachers', 'training/TrainingTeacherBank', 'train:teacher:view', 'SolutionOutlined', 14, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4815, 1, 4300, 'page', 'train_files', '档案管理', '/training/files', 'training/TrainingFileManagement', 'train:file:view', 'FolderOutlined', 15, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- Permissions for each training sub-module
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  -- Train Needs (培训需求) - menu 4801
  (5300, 1, 4801, 'train:need:view', '查看培训需求', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5301, 1, 4801, 'train:need:create', '新增培训需求', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5302, 1, 4801, 'train:need:update', '编辑培训需求', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5303, 1, 4801, 'train:need:delete', '删除培训需求', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Solutions (培训方案) - menu 4802
  (5304, 1, 4802, 'train:solution:view', '查看培训方案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5305, 1, 4802, 'train:solution:create', '新增培训方案', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5306, 1, 4802, 'train:solution:update', '编辑培训方案', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5307, 1, 4802, 'train:solution:delete', '删除培训方案', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Requests (培训请求) - menu 4803
  (5308, 1, 4803, 'train:request:view', '查看培训请求', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5309, 1, 4803, 'train:request:create', '新增培训请求', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5310, 1, 4803, 'train:request:update', '编辑培训请求', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5311, 1, 4803, 'train:request:delete', '删除培训请求', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Declares (培训申报) - menu 4804
  (5312, 1, 4804, 'train:declare:view', '查看培训申报', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5313, 1, 4804, 'train:declare:create', '新增培训申报', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5314, 1, 4804, 'train:declare:update', '编辑培训申报', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5315, 1, 4804, 'train:declare:delete', '删除培训申报', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train People (培训人员) - menu 4805
  (5316, 1, 4805, 'train:people:view', '查看培训人员', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5317, 1, 4805, 'train:people:create', '新增培训人员', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5318, 1, 4805, 'train:people:update', '编辑培训人员', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5319, 1, 4805, 'train:people:delete', '删除培训人员', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Classes (班级管理) - menu 4806
  (5320, 1, 4806, 'train:class:view', '查看班级', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5321, 1, 4806, 'train:class:create', '新增班级', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5322, 1, 4806, 'train:class:update', '编辑班级', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5323, 1, 4806, 'train:class:delete', '删除班级', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Courses (课程管理) - menu 4807
  (5324, 1, 4807, 'train:course:view', '查看课程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5325, 1, 4807, 'train:course:create', '新增课程', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5326, 1, 4807, 'train:course:update', '编辑课程', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5327, 1, 4807, 'train:course:delete', '删除课程', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Resources (资源管理) - menu 4808
  (5328, 1, 4808, 'train:resource:view', '查看资源', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5329, 1, 4808, 'train:resource:create', '新增资源', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5330, 1, 4808, 'train:resource:update', '编辑资源', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5331, 1, 4808, 'train:resource:delete', '删除资源', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Tasks (任务管理) - menu 4809
  (5332, 1, 4809, 'train:task:view', '查看任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5333, 1, 4809, 'train:task:create', '新增任务', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5334, 1, 4809, 'train:task:update', '编辑任务', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5335, 1, 4809, 'train:task:delete', '删除任务', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Exams (考试管理) - menu 4810
  (5336, 1, 4810, 'train:exam:view', '查看考试', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5337, 1, 4810, 'train:exam:create', '新增考试', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5338, 1, 4810, 'train:exam:update', '编辑考试', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5339, 1, 4810, 'train:exam:delete', '删除考试', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Budgets (预算管理) - menu 4811
  (5340, 1, 4811, 'train:budget:view', '查看预算', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5341, 1, 4811, 'train:budget:create', '新增预算', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5342, 1, 4811, 'train:budget:update', '编辑预算', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5343, 1, 4811, 'train:budget:delete', '删除预算', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Certificates (认定管理) - menu 4812
  (5344, 1, 4812, 'train:certificate:view', '查看证书', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5345, 1, 4812, 'train:certificate:create', '新增证书', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5346, 1, 4812, 'train:certificate:update', '编辑证书', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5347, 1, 4812, 'train:certificate:delete', '删除证书', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Talent Needs (人才需求) - menu 4813
  (5348, 1, 4813, 'train:talent:view', '查看人才需求', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5349, 1, 4813, 'train:talent:create', '新增人才需求', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5350, 1, 4813, 'train:talent:update', '编辑人才需求', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5351, 1, 4813, 'train:talent:delete', '删除人才需求', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Teachers (教师库) - menu 4814
  (5352, 1, 4814, 'train:teacher:view', '查看教师', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5353, 1, 4814, 'train:teacher:create', '新增教师', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5354, 1, 4814, 'train:teacher:update', '编辑教师', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5355, 1, 4814, 'train:teacher:delete', '删除教师', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- Train Files (档案管理) - menu 4815
  (5356, 1, 4815, 'train:file:view', '查看档案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5357, 1, 4815, 'train:file:create', '新增档案', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5358, 1, 4815, 'train:file:update', '编辑档案', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5359, 1, 4815, 'train:file:delete', '删除档案', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant menus to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604700, 1, 2000, 4801, NOW(), NOW(), 1, 1, 0),
  (604701, 1, 2000, 4802, NOW(), NOW(), 1, 1, 0),
  (604702, 1, 2000, 4803, NOW(), NOW(), 1, 1, 0),
  (604703, 1, 2000, 4804, NOW(), NOW(), 1, 1, 0),
  (604704, 1, 2000, 4805, NOW(), NOW(), 1, 1, 0),
  (604705, 1, 2000, 4806, NOW(), NOW(), 1, 1, 0),
  (604706, 1, 2000, 4807, NOW(), NOW(), 1, 1, 0),
  (604707, 1, 2000, 4808, NOW(), NOW(), 1, 1, 0),
  (604708, 1, 2000, 4809, NOW(), NOW(), 1, 1, 0),
  (604709, 1, 2000, 4810, NOW(), NOW(), 1, 1, 0),
  (604710, 1, 2000, 4811, NOW(), NOW(), 1, 1, 0),
  (604711, 1, 2000, 4812, NOW(), NOW(), 1, 1, 0),
  (604712, 1, 2000, 4813, NOW(), NOW(), 1, 1, 0),
  (604713, 1, 2000, 4814, NOW(), NOW(), 1, 1, 0),
  (604714, 1, 2000, 4815, NOW(), NOW(), 1, 1, 0);

-- Grant permissions to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705200, 1, 2000, 5300, NOW(), NOW(), 1, 1, 0),
  (705201, 1, 2000, 5301, NOW(), NOW(), 1, 1, 0),
  (705202, 1, 2000, 5302, NOW(), NOW(), 1, 1, 0),
  (705203, 1, 2000, 5303, NOW(), NOW(), 1, 1, 0),
  (705204, 1, 2000, 5304, NOW(), NOW(), 1, 1, 0),
  (705205, 1, 2000, 5305, NOW(), NOW(), 1, 1, 0),
  (705206, 1, 2000, 5306, NOW(), NOW(), 1, 1, 0),
  (705207, 1, 2000, 5307, NOW(), NOW(), 1, 1, 0),
  (705208, 1, 2000, 5308, NOW(), NOW(), 1, 1, 0),
  (705209, 1, 2000, 5309, NOW(), NOW(), 1, 1, 0),
  (705210, 1, 2000, 5310, NOW(), NOW(), 1, 1, 0),
  (705211, 1, 2000, 5311, NOW(), NOW(), 1, 1, 0),
  (705212, 1, 2000, 5312, NOW(), NOW(), 1, 1, 0),
  (705213, 1, 2000, 5313, NOW(), NOW(), 1, 1, 0),
  (705214, 1, 2000, 5314, NOW(), NOW(), 1, 1, 0),
  (705215, 1, 2000, 5315, NOW(), NOW(), 1, 1, 0),
  (705216, 1, 2000, 5316, NOW(), NOW(), 1, 1, 0),
  (705217, 1, 2000, 5317, NOW(), NOW(), 1, 1, 0),
  (705218, 1, 2000, 5318, NOW(), NOW(), 1, 1, 0),
  (705219, 1, 2000, 5319, NOW(), NOW(), 1, 1, 0),
  (705220, 1, 2000, 5320, NOW(), NOW(), 1, 1, 0),
  (705221, 1, 2000, 5321, NOW(), NOW(), 1, 1, 0),
  (705222, 1, 2000, 5322, NOW(), NOW(), 1, 1, 0),
  (705223, 1, 2000, 5323, NOW(), NOW(), 1, 1, 0),
  (705224, 1, 2000, 5324, NOW(), NOW(), 1, 1, 0),
  (705225, 1, 2000, 5325, NOW(), NOW(), 1, 1, 0),
  (705226, 1, 2000, 5326, NOW(), NOW(), 1, 1, 0),
  (705227, 1, 2000, 5327, NOW(), NOW(), 1, 1, 0),
  (705228, 1, 2000, 5328, NOW(), NOW(), 1, 1, 0),
  (705229, 1, 2000, 5329, NOW(), NOW(), 1, 1, 0),
  (705230, 1, 2000, 5330, NOW(), NOW(), 1, 1, 0),
  (705231, 1, 2000, 5331, NOW(), NOW(), 1, 1, 0),
  (705232, 1, 2000, 5332, NOW(), NOW(), 1, 1, 0),
  (705233, 1, 2000, 5333, NOW(), NOW(), 1, 1, 0),
  (705234, 1, 2000, 5334, NOW(), NOW(), 1, 1, 0),
  (705235, 1, 2000, 5335, NOW(), NOW(), 1, 1, 0),
  (705236, 1, 2000, 5336, NOW(), NOW(), 1, 1, 0),
  (705237, 1, 2000, 5337, NOW(), NOW(), 1, 1, 0),
  (705238, 1, 2000, 5338, NOW(), NOW(), 1, 1, 0),
  (705239, 1, 2000, 5339, NOW(), NOW(), 1, 1, 0),
  (705240, 1, 2000, 5340, NOW(), NOW(), 1, 1, 0),
  (705241, 1, 2000, 5341, NOW(), NOW(), 1, 1, 0),
  (705242, 1, 2000, 5342, NOW(), NOW(), 1, 1, 0),
  (705243, 1, 2000, 5343, NOW(), NOW(), 1, 1, 0),
  (705244, 1, 2000, 5344, NOW(), NOW(), 1, 1, 0),
  (705245, 1, 2000, 5345, NOW(), NOW(), 1, 1, 0),
  (705246, 1, 2000, 5346, NOW(), NOW(), 1, 1, 0),
  (705247, 1, 2000, 5347, NOW(), NOW(), 1, 1, 0),
  (705248, 1, 2000, 5348, NOW(), NOW(), 1, 1, 0),
  (705249, 1, 2000, 5349, NOW(), NOW(), 1, 1, 0),
  (705250, 1, 2000, 5350, NOW(), NOW(), 1, 1, 0),
  (705251, 1, 2000, 5351, NOW(), NOW(), 1, 1, 0),
  (705252, 1, 2000, 5352, NOW(), NOW(), 1, 1, 0),
  (705253, 1, 2000, 5353, NOW(), NOW(), 1, 1, 0),
  (705254, 1, 2000, 5354, NOW(), NOW(), 1, 1, 0),
  (705255, 1, 2000, 5355, NOW(), NOW(), 1, 1, 0),
  (705256, 1, 2000, 5356, NOW(), NOW(), 1, 1, 0),
  (705257, 1, 2000, 5357, NOW(), NOW(), 1, 1, 0),
  (705258, 1, 2000, 5358, NOW(), NOW(), 1, 1, 0),
  (705259, 1, 2000, 5359, NOW(), NOW(), 1, 1, 0);
