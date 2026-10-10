-- Add zhixun (智训平台) menu directory and all sub-pages for demo tenant
-- Parent directory
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5100, 1, 0, 'directory', 'zhixun', '智训平台', '/zhixun', NULL, 'zhixun:view', 'ReadOutlined', 25, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- Sub-pages under zhixun directory (parent_id=5100)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5101, 1, 5100, 'page', 'zhixun_data_panel', '数据面板', '/zhixun/data-panel', 'zhixunplatform/DataPanel', 'zhixun:data-panel:view', 'DashboardOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5102, 1, 5100, 'page', 'zhixun_task_management', '任务管理', '/zhixun/task-management', 'zhixunplatform/TaskManagement', 'zhixun:task-management:view', 'UnorderedListOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5103, 1, 5100, 'page', 'zhixun_task_archives', '任务档案', '/zhixun/task-archives', 'zhixunplatform/TaskArchives', 'zhixun:task-archives:view', 'FolderOpenOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5104, 1, 5100, 'page', 'zhixun_training_process', '培训流程', '/zhixun/training-process', 'zhixunplatform/TrainingProcess', 'zhixun:training-process:view', 'NodeIndexOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5105, 1, 5100, 'page', 'zhixun_project_archives', '项目档案', '/zhixun/project-archives', 'zhixunplatform/ProjectArchives', 'zhixun:project-archives:view', 'FolderOpenOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5106, 1, 5100, 'page', 'zhixun_material_archives', '资料档案', '/zhixun/material-archives', 'zhixunplatform/MaterialArchives', 'zhixun:material-archives:view', 'FileTextOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5107, 1, 5100, 'page', 'zhixun_talent_identification', '人才鉴定', '/zhixun/talent-identification', 'zhixunplatform/TalentIdentification', 'zhixun:talent-identification:view', 'TeamOutlined', 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5108, 1, 5100, 'page', 'zhixun_preliminary_plan', '初步方案', '/zhixun/preliminary-plan', 'zhixunplatform/PreliminaryPlan', 'zhixun:preliminary-plan:view', 'SolutionOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5109, 1, 5100, 'page', 'zhixun_create_task', '创建任务', '/zhixun/create-task', 'zhixunplatform/CreateTask', 'zhixun:create-task:view', 'FileAddOutlined', 9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5110, 1, 5100, 'page', 'zhixun_final_plan', '最终方案', '/zhixun/final-plan', 'zhixunplatform/FinalPlan', 'zhixun:final-plan:view', 'FileTextOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5111, 1, 5100, 'page', 'zhixun_class_declaration', '班级申报', '/zhixun/class-declaration', 'zhixunplatform/ClassDeclaration', 'zhixun:class-declaration:view', 'AuditOutlined', 11, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5112, 1, 5100, 'page', 'zhixun_training_implementation', '培训实施', '/zhixun/training-implementation', 'zhixunplatform/TrainingImplementation', 'zhixun:training-implementation:view', 'ScheduleOutlined', 12, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5113, 1, 5100, 'page', 'zhixun_certification', '认证', '/zhixun/certification', 'zhixunplatform/Certification', 'zhixun:certification:view', 'SafetyCertificateOutlined', 13, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5114, 1, 5100, 'page', 'zhixun_declaration_service', '申报服务', '/zhixun/declaration-service', 'zhixunplatform/DeclarationService', 'zhixun:declaration-service:view', 'ApiOutlined', 14, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5115, 1, 5100, 'page', 'zhixun_graduation', '毕业', '/zhixun/graduation', 'zhixunplatform/Graduation', 'zhixun:graduation:view', 'IdcardOutlined', 15, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5116, 1, 5100, 'page', 'zhixun_refund', '退款', '/zhixun/refund', 'zhixunplatform/Refund', 'zhixun:refund:view', 'DollarOutlined', 16, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- Permissions for each zhixun sub-page
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5700, 1, 5100, 'zhixun:view', '查看智训平台', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5701, 1, 5101, 'zhixun:data-panel:view', '查看数据面板', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5702, 1, 5102, 'zhixun:task-management:view', '查看任务管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5703, 1, 5103, 'zhixun:task-archives:view', '查看任务档案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5704, 1, 5104, 'zhixun:training-process:view', '查看培训流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5705, 1, 5105, 'zhixun:project-archives:view', '查看项目档案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5706, 1, 5106, 'zhixun:material-archives:view', '查看资料档案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5707, 1, 5107, 'zhixun:talent-identification:view', '查看人才鉴定', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5708, 1, 5108, 'zhixun:preliminary-plan:view', '查看初步方案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5709, 1, 5109, 'zhixun:create-task:view', '查看创建任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5710, 1, 5110, 'zhixun:final-plan:view', '查看最终方案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5711, 1, 5111, 'zhixun:class-declaration:view', '查看班级申报', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5712, 1, 5112, 'zhixun:training-implementation:view', '查看培训实施', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5713, 1, 5113, 'zhixun:certification:view', '查看认证', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5714, 1, 5114, 'zhixun:declaration-service:view', '查看申报服务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5715, 1, 5115, 'zhixun:graduation:view', '查看毕业', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5716, 1, 5116, 'zhixun:refund:view', '查看退款', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant all zhixun menus to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (605000, 1, 2000, 5100, NOW(), NOW(), 1, 1, 0),
  (605001, 1, 2000, 5101, NOW(), NOW(), 1, 1, 0),
  (605002, 1, 2000, 5102, NOW(), NOW(), 1, 1, 0),
  (605003, 1, 2000, 5103, NOW(), NOW(), 1, 1, 0),
  (605004, 1, 2000, 5104, NOW(), NOW(), 1, 1, 0),
  (605005, 1, 2000, 5105, NOW(), NOW(), 1, 1, 0),
  (605006, 1, 2000, 5106, NOW(), NOW(), 1, 1, 0),
  (605007, 1, 2000, 5107, NOW(), NOW(), 1, 1, 0),
  (605008, 1, 2000, 5108, NOW(), NOW(), 1, 1, 0),
  (605009, 1, 2000, 5109, NOW(), NOW(), 1, 1, 0),
  (605010, 1, 2000, 5110, NOW(), NOW(), 1, 1, 0),
  (605011, 1, 2000, 5111, NOW(), NOW(), 1, 1, 0),
  (605012, 1, 2000, 5112, NOW(), NOW(), 1, 1, 0),
  (605013, 1, 2000, 5113, NOW(), NOW(), 1, 1, 0),
  (605014, 1, 2000, 5114, NOW(), NOW(), 1, 1, 0),
  (605015, 1, 2000, 5115, NOW(), NOW(), 1, 1, 0),
  (605016, 1, 2000, 5116, NOW(), NOW(), 1, 1, 0);

-- Grant all zhixun permissions to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (706000, 1, 2000, 5700, NOW(), NOW(), 1, 1, 0),
  (706001, 1, 2000, 5701, NOW(), NOW(), 1, 1, 0),
  (706002, 1, 2000, 5702, NOW(), NOW(), 1, 1, 0),
  (706003, 1, 2000, 5703, NOW(), NOW(), 1, 1, 0),
  (706004, 1, 2000, 5704, NOW(), NOW(), 1, 1, 0),
  (706005, 1, 2000, 5705, NOW(), NOW(), 1, 1, 0),
  (706006, 1, 2000, 5706, NOW(), NOW(), 1, 1, 0),
  (706007, 1, 2000, 5707, NOW(), NOW(), 1, 1, 0),
  (706008, 1, 2000, 5708, NOW(), NOW(), 1, 1, 0),
  (706009, 1, 2000, 5709, NOW(), NOW(), 1, 1, 0),
  (706010, 1, 2000, 5710, NOW(), NOW(), 1, 1, 0),
  (706011, 1, 2000, 5711, NOW(), NOW(), 1, 1, 0),
  (706012, 1, 2000, 5712, NOW(), NOW(), 1, 1, 0),
  (706013, 1, 2000, 5713, NOW(), NOW(), 1, 1, 0),
  (706014, 1, 2000, 5714, NOW(), NOW(), 1, 1, 0),
  (706015, 1, 2000, 5715, NOW(), NOW(), 1, 1, 0),
  (706016, 1, 2000, 5716, NOW(), NOW(), 1, 1, 0);
