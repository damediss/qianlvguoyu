INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5100, 1, 0, 'directory', 'industrial', '工业互联网平台', '/industrial', NULL, NULL, 'ClusterOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5101, 1, 5100, 'page', 'industrial_college', '园区学院', '/industrial/college', 'industrial/College', 'industrial:college:view', 'BankOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5700, 1, 5101, 'industrial:college:view', '查看园区学院', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5701, 1, 5101, 'industrial:college:create', '新增园区学院', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5702, 1, 5101, 'industrial:college:update', '编辑园区学院', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5703, 1, 5101, 'industrial:college:delete', '删除园区学院', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (606100, 1, 2000, 5100, NOW(), NOW(), 1, 1, 0),
  (606101, 1, 2000, 5101, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (706100, 1, 2000, 5700, NOW(), NOW(), 1, 1, 0),
  (706101, 1, 2000, 5701, NOW(), NOW(), 1, 1, 0),
  (706102, 1, 2000, 5702, NOW(), NOW(), 1, 1, 0),
  (706103, 1, 2000, 5703, NOW(), NOW(), 1, 1, 0);
