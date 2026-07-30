CREATE TABLE IF NOT EXISTS industrial_talent_training (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  name VARCHAR(200) NOT NULL COMMENT '企业名称',
  level VARCHAR(80) NOT NULL COMMENT '职称/技能等级',
  work_type VARCHAR(120) NOT NULL COMMENT '工种',
  zhiwei VARCHAR(120) NOT NULL COMMENT '岗位',
  education VARCHAR(120) NOT NULL COMMENT '学历要求',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_industrial_talent_training_tenant (tenant_id, deleted),
  KEY idx_industrial_talent_training_work_type (tenant_id, work_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工业互联网人才培养';

CREATE TABLE IF NOT EXISTS industrial_talent_service (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  title VARCHAR(300) NOT NULL COMMENT '服务标题',
  source VARCHAR(120) NOT NULL COMMENT '来源',
  service_date VARCHAR(40) NOT NULL COMMENT '开始日期',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_industrial_talent_service_tenant (tenant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工业互联网人才服务';

INSERT IGNORE INTO industrial_talent_training (
  id, tenant_id, name, level, work_type, zhiwei, education, sort_order, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800001, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 1, NOW(), NOW(), 1, 1, 0),
  (800002, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 2, NOW(), NOW(), 1, 1, 0),
  (800003, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 3, NOW(), NOW(), 1, 1, 0),
  (800004, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 4, NOW(), NOW(), 1, 1, 0),
  (800005, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 5, NOW(), NOW(), 1, 1, 0),
  (800006, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 6, NOW(), NOW(), 1, 1, 0),
  (800007, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 7, NOW(), NOW(), 1, 1, 0),
  (800008, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 8, NOW(), NOW(), 1, 1, 0),
  (800009, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 9, NOW(), NOW(), 1, 1, 0),
  (800010, 1, '千铝培训', '高级工', '电解槽操作工', '班组长', '大专及以上', 10, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO industrial_talent_service (
  id, tenant_id, title, source, service_date, sort_order, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800101, 1, '2025.8.5技师认定交流会', '千铝工投', '2025.9.10', 1, NOW(), NOW(), 1, 1, 0),
  (800102, 1, '高级技师论文职称指导', '千铝工投', '2025.9.10', 2, NOW(), NOW(), 1, 1, 0),
  (800103, 1, '2025.8.5中级证书补贴申报指导', '千铝工投', '2025.9.10', 3, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5131, 1, 5100, 'page', 'industrial_talent_training', '人才培养', '/industrial/talent/training', 'industrial/talent/Training', 'industrial:talent-training:view', 'SolutionOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5132, 1, 5100, 'page', 'industrial_talent_service', '人才服务', '/industrial/talent/service', 'industrial/talent/Service', 'industrial:talent-service:view', 'TeamOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5770, 1, 5131, 'industrial:talent-training:view', '查看人才培养', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5771, 1, 5132, 'industrial:talent-service:view', '查看人才服务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (606131, 1, 2000, 5131, NOW(), NOW(), 1, 1, 0),
  (606132, 1, 2000, 5132, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (706170, 1, 2000, 5770, NOW(), NOW(), 1, 1, 0),
  (706171, 1, 2000, 5771, NOW(), NOW(), 1, 1, 0);
