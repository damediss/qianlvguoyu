-- ============================================================
-- 战略规划 → 产品规划 菜单树
-- 插入 sys_menu、sys_permission、sys_role_menu、sys_role_permission
-- ============================================================

-- 1. 战略规划 顶层目录 (parent_id=0, sort=5, 排在 dashboard 之后)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8000, 1, 0, 'directory', 'strate_plan', '战略规划', '/strate', NULL, 'strate:view', 'RocketOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 人才标准
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8001, 1, 8000, 'page', 'strate_talent_standard', '人才标准', '/strate/talent-standard', 'product/ProductPlaceholder', 'strate.talent_standard:view', 'IdcardOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 人才盘点
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8002, 1, 8000, 'page', 'strate_talent_check', '人才盘点', '/strate/talent-check', 'product/ProductPlaceholder', 'strate.talent_check:view', 'PieChartOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 产品规划 目录
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8003, 1, 8000, 'directory', 'strate_product_planning', '产品规划', '/strate/product-planning', NULL, 'strate.product:view', 'AppstoreOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 24 个产品模块 (parent_id=8003)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8004, 1, 8003, 'page', 'product_ai_industry',       'AI规划（行业）',          '/product/ai-industry',       'product/ProductPlaceholder', 'product.ai_industry:view',       'BulbOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8005, 1, 8003, 'page', 'product_ai_college',        'AI规划（院校）',          '/product/ai-college',        'product/ProductPlaceholder', 'product.ai_college:view',        'BulbOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8006, 1, 8003, 'page', 'product_ai_enterprise',     'AI规划（企业）',          '/product/ai-enterprise',     'product/ProductPlaceholder', 'product.ai_enterprise:view',     'BulbOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8007, 1, 8003, 'page', 'product_ai_individual',     'AI规划（个人）',          '/product/ai-individual',     'product/ProductPlaceholder', 'product.ai_individual:view',     'BulbOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8008, 1, 8003, 'page', 'product_course_dev',        '课程开发系统',            '/product/course-dev',        'product/ProductPlaceholder', 'product.course_dev:view',        'BookOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8009, 1, 8003, 'page', 'product_knowledge_nonferrous', '有色培鉴赛知识库',       '/product/knowledge-nonferrous', 'product/ProductPlaceholder', 'product.knowledge_nonferrous:view', 'DatabaseOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8010, 1, 8003, 'page', 'product_qianlv_ecosystem',  '千铝技能生态系统',        '/product/qianlv-ecosystem',  'product/ProductPlaceholder', 'product.qianlv_ecosystem:view',  'GlobalOutlined', 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8011, 1, 8003, 'page', 'product_simulation',        '培鉴赛仿真平台',          '/product/simulation',        'product/ProductPlaceholder', 'product.simulation:view',        'ExperimentOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8012, 1, 8003, 'page', 'product_training_base',     '培训基地运营系统',        '/product/training-base',     'product/ProductPlaceholder', 'product.training_base:view',     'HomeOutlined', 9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8013, 1, 8003, 'page', 'product_apprentice',        '新型学徒运营系统',        '/product/apprentice',        'product/ProductPlaceholder', 'product.apprentice:view',        'TeamOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8014, 1, 8003, 'page', 'product_cert_agency',       '自主认定机构运营系统',    '/product/cert-agency',       'product/ProductPlaceholder', 'product.cert_agency:view',       'SafetyCertificateOutlined', 11, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8015, 1, 8003, 'page', 'product_competition',       '国行省市赛运营系统',      '/product/competition',       'product/ProductPlaceholder', 'product.competition:view',       'TrophyOutlined', 12, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8016, 1, 8003, 'page', 'product_studio_apply',      '工作室申报运营系统',      '/product/studio-apply',      'product/ProductPlaceholder', 'product.studio_apply:view',      'FormOutlined', 13, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8017, 1, 8003, 'page', 'product_course_ops',        '课程开发运营系统',        '/product/course-ops',        'product/ProductPlaceholder', 'product.course_ops:view',        'BookOutlined', 14, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8018, 1, 8003, 'page', 'product_ai_nonferrous_assess', '有色考评AI智能体',       '/product/ai-nonferrous-assess', 'product/ProductPlaceholder', 'product.ai_nonferrous_assess:view', 'RobotOutlined', 15, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8019, 1, 8003, 'page', 'product_qianlv_knowledge',  '千铝知识库',              '/product/qianlv-knowledge',  'product/ProductPlaceholder', 'product.qianlv_knowledge:view',  'ReadOutlined', 16, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8020, 1, 8003, 'page', 'product_smart_classroom',   '有色智慧教室',            '/product/smart-classroom',   'product/ProductPlaceholder', 'product.smart_classroom:view',   'DesktopOutlined', 17, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8021, 1, 8003, 'page', 'product_expert_system',     '职责能力建设专家库系统',  '/product/expert-system',     'product/ProductPlaceholder', 'product.expert_system:view',     'UserSwitchOutlined', 18, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8022, 1, 8003, 'page', 'product_ai_learning',       'AI学（同步精准专项学）',  '/product/ai-learning',       'product/ProductPlaceholder', 'product.ai_learning:view',       'PlayCircleOutlined', 19, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8023, 1, 8003, 'page', 'product_ai_assess',         'AI考评',                  '/product/ai-assess',         'product/ProductPlaceholder', 'product.ai_assess:view',         'CheckCircleOutlined', 20, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8024, 1, 8003, 'page', 'product_ai_agency_apply',   'AI机构申报',              '/product/ai-agency-apply',   'product/ProductPlaceholder', 'product.ai_agency_apply:view',   'FileProtectOutlined', 21, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8025, 1, 8003, 'page', 'product_ai_individual_apply','AI个人申报',              '/product/ai-individual-apply','product/ProductPlaceholder', 'product.ai_individual_apply:view','SolutionOutlined', 22, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8026, 1, 8003, 'page', 'product_ai_class_manage',   'AI班级管理',              '/product/ai-class-manage',   'product/ProductPlaceholder', 'product.ai_class_manage:view',   'GroupOutlined', 23, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (8027, 1, 8003, 'page', 'product_ai_skill_manage',   'AI技能管理',              '/product/ai-skill-manage',   'product/ProductPlaceholder', 'product.ai_skill_manage:view',   'ToolOutlined', 24, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 6. 权限 (sys_permission) —— 为每个叶子菜单生成 view 权限
-- ============================================================
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (8100, 1, 8000, 'strate:view',                '查看战略规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8101, 1, 8001, 'strate.talent_standard:view', '查看人才标准',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8102, 1, 8002, 'strate.talent_check:view',    '查看人才盘点',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8103, 1, 8003, 'strate.product:view',         '查看产品规划',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8104, 1, 8004, 'product.ai_industry:view',        '查看AI规划（行业）',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8105, 1, 8005, 'product.ai_college:view',         '查看AI规划（院校）',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8106, 1, 8006, 'product.ai_enterprise:view',      '查看AI规划（企业）',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8107, 1, 8007, 'product.ai_individual:view',      '查看AI规划（个人）',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8108, 1, 8008, 'product.course_dev:view',         '查看课程开发系统',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8109, 1, 8009, 'product.knowledge_nonferrous:view','查看有色培鉴赛知识库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8110, 1, 8010, 'product.qianlv_ecosystem:view',   '查看千铝技能生态系统',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8111, 1, 8011, 'product.simulation:view',         '查看培鉴赛仿真平台',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8112, 1, 8012, 'product.training_base:view',      '查看培训基地运营系统',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8113, 1, 8013, 'product.apprentice:view',         '查看新型学徒运营系统',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8114, 1, 8014, 'product.cert_agency:view',        '查看自主认定机构运营系统',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8115, 1, 8015, 'product.competition:view',        '查看国行省市赛运营系统',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8116, 1, 8016, 'product.studio_apply:view',       '查看工作室申报运营系统',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8117, 1, 8017, 'product.course_ops:view',         '查看课程开发运营系统',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8118, 1, 8018, 'product.ai_nonferrous_assess:view','查看有色考评AI智能体',        'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8119, 1, 8019, 'product.qianlv_knowledge:view',   '查看千铝知识库',             'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8120, 1, 8020, 'product.smart_classroom:view',    '查看有色智慧教室',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8121, 1, 8021, 'product.expert_system:view',      '查看职责能力建设专家库系统', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8122, 1, 8022, 'product.ai_learning:view',        '查看AI学',                   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8123, 1, 8023, 'product.ai_assess:view',          '查看AI考评',                 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8124, 1, 8024, 'product.ai_agency_apply:view',    '查看AI机构申报',             'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8125, 1, 8025, 'product.ai_individual_apply:view','查看AI个人申报',             'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8126, 1, 8026, 'product.ai_class_manage:view',    '查看AI班级管理',             'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (8127, 1, 8027, 'product.ai_skill_manage:view',    '查看AI技能管理',             'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- 7. 角色-菜单关联 (sys_role_menu) —— 赋给平台管理员 role_id=2000
-- ============================================================
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 800000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8000 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 8001, 1 UNION ALL SELECT 8002, 1 UNION ALL SELECT 8003, 1 UNION ALL
  SELECT 8004, 1 UNION ALL SELECT 8005, 1 UNION ALL SELECT 8006, 1 UNION ALL
  SELECT 8007, 1 UNION ALL SELECT 8008, 1 UNION ALL SELECT 8009, 1 UNION ALL
  SELECT 8010, 1 UNION ALL SELECT 8011, 1 UNION ALL SELECT 8012, 1 UNION ALL
  SELECT 8013, 1 UNION ALL SELECT 8014, 1 UNION ALL SELECT 8015, 1 UNION ALL
  SELECT 8016, 1 UNION ALL SELECT 8017, 1 UNION ALL SELECT 8018, 1 UNION ALL
  SELECT 8019, 1 UNION ALL SELECT 8020, 1 UNION ALL SELECT 8021, 1 UNION ALL
  SELECT 8022, 1 UNION ALL SELECT 8023, 1 UNION ALL SELECT 8024, 1 UNION ALL
  SELECT 8025, 1 UNION ALL SELECT 8026, 1 UNION ALL SELECT 8027, 1
) t;

-- ============================================================
-- 8. 角色-权限关联 (sys_role_permission) —— 赋给平台管理员 role_id=2000
-- ============================================================
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 810000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 8100 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 8101, 1 UNION ALL SELECT 8102, 1 UNION ALL SELECT 8103, 1 UNION ALL
  SELECT 8104, 1 UNION ALL SELECT 8105, 1 UNION ALL SELECT 8106, 1 UNION ALL
  SELECT 8107, 1 UNION ALL SELECT 8108, 1 UNION ALL SELECT 8109, 1 UNION ALL
  SELECT 8110, 1 UNION ALL SELECT 8111, 1 UNION ALL SELECT 8112, 1 UNION ALL
  SELECT 8113, 1 UNION ALL SELECT 8114, 1 UNION ALL SELECT 8115, 1 UNION ALL
  SELECT 8116, 1 UNION ALL SELECT 8117, 1 UNION ALL SELECT 8118, 1 UNION ALL
  SELECT 8119, 1 UNION ALL SELECT 8120, 1 UNION ALL SELECT 8121, 1 UNION ALL
  SELECT 8122, 1 UNION ALL SELECT 8123, 1 UNION ALL SELECT 8124, 1 UNION ALL
  SELECT 8125, 1 UNION ALL SELECT 8126, 1 UNION ALL SELECT 8127, 1
) t;