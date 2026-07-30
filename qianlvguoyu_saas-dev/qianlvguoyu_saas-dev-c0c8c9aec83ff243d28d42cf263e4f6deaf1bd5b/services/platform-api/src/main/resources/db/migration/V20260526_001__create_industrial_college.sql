CREATE TABLE IF NOT EXISTS industrial_college (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  name VARCHAR(200) NOT NULL COMMENT '机构名称',
  tag VARCHAR(80) NULL COMMENT '类型',
  business_scope VARCHAR(1000) NULL COMMENT '经营范围',
  description TEXT NULL COMMENT '简介',
  training_qualification VARCHAR(1000) NULL COMMENT '培训资质',
  certification_qualification VARCHAR(1000) NULL COMMENT '认定资质',
  address VARCHAR(300) NULL COMMENT '地址',
  phone VARCHAR(50) NULL COMMENT '联系电话',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_industrial_college_tenant_status (tenant_id, status, deleted),
  KEY idx_industrial_college_tag (tenant_id, tag, deleted),
  KEY idx_industrial_college_created_at (tenant_id, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='园区学院';

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
