INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6390, 1, 6300, 'directory', 'certification_strategy_execution', '战略执行',
   '/certification/strategy-execution', NULL, NULL, 'DashboardOutlined', 1, 1,
   'enabled', NOW(), NOW(), 1, 1, 0),
  (6391, 1, 6300, 'directory', 'certification_operation_flow', '运营流程',
   '/certification/operation-flow', NULL, NULL, 'BranchesOutlined', 2, 1,
   'enabled', NOW(), NOW(), 1, 1, 0),
  (6392, 1, 6300, 'directory', 'certification_support_assurance', '支持保障',
   '/certification/support-assurance', NULL, NULL, 'FolderOutlined', 3, 1,
   'enabled', NOW(), NOW(), 1, 1, 0),
  (6393, 1, 6300, 'directory', 'certification_performance_talent', '绩效人才',
   '/certification/performance-talent', NULL, NULL, 'TeamOutlined', 4, 1,
   'enabled', NOW(), NOW(), 1, 1, 0),
  (6394, 1, 6300, 'directory', 'certification_risk_compliance', '风控合规',
   '/certification/risk-compliance', NULL, NULL, 'SafetyOutlined', 5, 1,
   'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET
  parent_id = CASE code
    WHEN 'certification_dashboard' THEN 6390
    WHEN 'certification_project' THEN 6391
    WHEN 'certification_preparation' THEN 6391
    WHEN 'certification_practical_collection' THEN 6391
    WHEN 'certification_practical_config' THEN 6391
    WHEN 'certification_archive' THEN 6391
    WHEN 'certification_info' THEN 6392
    WHEN 'certification_task_archive' THEN 6394
    ELSE parent_id
  END,
  name = CASE code
    WHEN 'certification_project' THEN '任务管理'
    ELSE name
  END,
  sort_order = CASE code
    WHEN 'certification_dashboard' THEN 1
    WHEN 'certification_project' THEN 1
    WHEN 'certification_preparation' THEN 2
    WHEN 'certification_practical_collection' THEN 3
    WHEN 'certification_practical_config' THEN 4
    WHEN 'certification_archive' THEN 5
    WHEN 'certification_info' THEN 1
    WHEN 'certification_task_archive' THEN 1
    ELSE sort_order
  END,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code IN (
    'certification_dashboard',
    'certification_project',
    'certification_preparation',
    'certification_practical_collection',
    'certification_practical_config',
    'certification_archive',
    'certification_info',
    'certification_task_archive'
  );
