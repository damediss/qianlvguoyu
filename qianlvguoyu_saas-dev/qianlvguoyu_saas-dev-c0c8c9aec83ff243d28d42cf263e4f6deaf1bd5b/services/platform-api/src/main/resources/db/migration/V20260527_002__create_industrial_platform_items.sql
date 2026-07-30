CREATE TABLE IF NOT EXISTS industrial_platform_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  category VARCHAR(40) NOT NULL COMMENT '平台建设分类: master/study/match/apprentice/comment',
  company VARCHAR(200) NOT NULL COMMENT '企业名称',
  name VARCHAR(200) NOT NULL COMMENT '名称',
  nums INT NULL COMMENT '人数',
  item_date DATE NULL COMMENT '业务日期',
  state VARCHAR(80) NULL COMMENT '状态',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_industrial_platform_category (tenant_id, category, deleted),
  KEY idx_industrial_platform_company (tenant_id, company, deleted),
  KEY idx_industrial_platform_date (tenant_id, item_date, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工业互联网平台建设数据';

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5110, 1, 5100, 'directory', 'industrial_platform_build', '平台建设', '/industrial/platform', NULL, NULL, 'ApartmentOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5111, 1, 5110, 'page', 'industrial_platform_master', '技能大师工作室', '/industrial/platform-master', 'industrial/PlatformMaster', 'industrial:platform-master:view', 'TeamOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5112, 1, 5110, 'page', 'industrial_platform_study', '学习型班组', '/industrial/platform-study', 'industrial/PlatformStudy', 'industrial:platform-study:view', 'ReadOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5113, 1, 5110, 'page', 'industrial_platform_match', '竞赛', '/industrial/platform-match', 'industrial/PlatformMatch', 'industrial:platform-match:view', 'TrophyOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5114, 1, 5110, 'page', 'industrial_platform_apprentice', '新型学徒', '/industrial/platform-apprentice', 'industrial/PlatformApprentice', 'industrial:platform-apprentice:view', 'SolutionOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5115, 1, 5110, 'page', 'industrial_platform_comment', '自主评价', '/industrial/platform-comment', 'industrial/PlatformComment', 'industrial:platform-comment:view', 'AuditOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5710, 1, 5111, 'industrial:platform-master:view', '查看技能大师工作室', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5711, 1, 5111, 'industrial:platform-master:create', '新增技能大师工作室', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5712, 1, 5111, 'industrial:platform-master:update', '编辑技能大师工作室', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5713, 1, 5111, 'industrial:platform-master:delete', '删除技能大师工作室', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5720, 1, 5112, 'industrial:platform-study:view', '查看学习型班组', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5721, 1, 5112, 'industrial:platform-study:create', '新增学习型班组', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5722, 1, 5112, 'industrial:platform-study:update', '编辑学习型班组', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5723, 1, 5112, 'industrial:platform-study:delete', '删除学习型班组', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5730, 1, 5113, 'industrial:platform-match:view', '查看竞赛', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5731, 1, 5113, 'industrial:platform-match:create', '新增竞赛', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5732, 1, 5113, 'industrial:platform-match:update', '编辑竞赛', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5733, 1, 5113, 'industrial:platform-match:delete', '删除竞赛', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5740, 1, 5114, 'industrial:platform-apprentice:view', '查看新型学徒', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5741, 1, 5114, 'industrial:platform-apprentice:create', '新增新型学徒', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5742, 1, 5114, 'industrial:platform-apprentice:update', '编辑新型学徒', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5743, 1, 5114, 'industrial:platform-apprentice:delete', '删除新型学徒', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5750, 1, 5115, 'industrial:platform-comment:view', '查看自主评价', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5751, 1, 5115, 'industrial:platform-comment:create', '新增自主评价', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5752, 1, 5115, 'industrial:platform-comment:update', '编辑自主评价', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5753, 1, 5115, 'industrial:platform-comment:delete', '删除自主评价', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (606110, 1, 2000, 5110, NOW(), NOW(), 1, 1, 0),
  (606111, 1, 2000, 5111, NOW(), NOW(), 1, 1, 0),
  (606112, 1, 2000, 5112, NOW(), NOW(), 1, 1, 0),
  (606113, 1, 2000, 5113, NOW(), NOW(), 1, 1, 0),
  (606114, 1, 2000, 5114, NOW(), NOW(), 1, 1, 0),
  (606115, 1, 2000, 5115, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (706110, 1, 2000, 5710, NOW(), NOW(), 1, 1, 0),
  (706111, 1, 2000, 5711, NOW(), NOW(), 1, 1, 0),
  (706112, 1, 2000, 5712, NOW(), NOW(), 1, 1, 0),
  (706113, 1, 2000, 5713, NOW(), NOW(), 1, 1, 0),
  (706120, 1, 2000, 5720, NOW(), NOW(), 1, 1, 0),
  (706121, 1, 2000, 5721, NOW(), NOW(), 1, 1, 0),
  (706122, 1, 2000, 5722, NOW(), NOW(), 1, 1, 0),
  (706123, 1, 2000, 5723, NOW(), NOW(), 1, 1, 0),
  (706130, 1, 2000, 5730, NOW(), NOW(), 1, 1, 0),
  (706131, 1, 2000, 5731, NOW(), NOW(), 1, 1, 0),
  (706132, 1, 2000, 5732, NOW(), NOW(), 1, 1, 0),
  (706133, 1, 2000, 5733, NOW(), NOW(), 1, 1, 0),
  (706140, 1, 2000, 5740, NOW(), NOW(), 1, 1, 0),
  (706141, 1, 2000, 5741, NOW(), NOW(), 1, 1, 0),
  (706142, 1, 2000, 5742, NOW(), NOW(), 1, 1, 0),
  (706143, 1, 2000, 5743, NOW(), NOW(), 1, 1, 0),
  (706150, 1, 2000, 5750, NOW(), NOW(), 1, 1, 0),
  (706151, 1, 2000, 5751, NOW(), NOW(), 1, 1, 0),
  (706152, 1, 2000, 5752, NOW(), NOW(), 1, 1, 0),
  (706153, 1, 2000, 5753, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO industrial_platform_item (
  id, tenant_id, category, company, name, nums, item_date, state, sort_order, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (520001, 1, 'master', '千铝国昱', '电解铝技能大师工作室', 12, NULL, NULL, 1, NOW(), NOW(), 1, 1, 0),
  (520002, 1, 'study', '千铝国昱', '电解一车间学习型班组', 28, NULL, NULL, 1, NOW(), NOW(), 1, 1, 0),
  (520003, 1, 'match', '千铝国昱', '工业互联网技能竞赛', NULL, '2026-05-27', NULL, 1, NOW(), NOW(), 1, 1, 0),
  (520004, 1, 'apprentice', '千铝国昱', '浸出工高级新型学徒开班', 30, '2026-05-27', NULL, 1, NOW(), NOW(), 1, 1, 0),
  (520005, 1, 'comment', '千铝国昱', '企业技能等级自主评价', NULL, '2026-05-27', '已完成', 1, NOW(), NOW(), 1, 1, 0);
