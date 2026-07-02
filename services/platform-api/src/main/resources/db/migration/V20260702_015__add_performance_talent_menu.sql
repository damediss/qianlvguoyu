-- ============================================================
-- 绩效人才 顶层菜单树
-- ============================================================

-- 1. 绩效人才 顶层目录
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9500, 1, 0, 'directory', 'perf_talent', '绩效人才', '/performance-talent', NULL, 'perf:view', 'CrownOutlined', 9, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 工作考核
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9501, 1, 9500, 'directory', 'perf_work_assess', '工作考核', '/performance-talent/work-assess', NULL, 'perf.work_assess:view', 'AuditOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9502, 1, 9501, 'page', 'perf_assess_structure',     '结构考核', '/performance-talent/work-assess/structure',     'product/ProductPlaceholder', 'perf.assess_structure:view',     'ApartmentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9503, 1, 9501, 'page', 'perf_assess_qualification', '资质考核', '/performance-talent/work-assess/qualification', 'product/ProductPlaceholder', 'perf.assess_qualification:view', 'SafetyCertificateOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9504, 1, 9501, 'page', 'perf_assess_resource',      '资源考核', '/performance-talent/work-assess/resource',      'product/ProductPlaceholder', 'perf.assess_resource:view',      'DatabaseOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 人才梯队
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9505, 1, 9500, 'directory', 'perf_talent_pipeline', '人才梯队', '/performance-talent/pipeline', NULL, 'perf.talent_pipeline:view', 'TeamOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9506, 1, 9505, 'page', 'perf_skill_map',      '技能图谱', '/performance-talent/pipeline/skill-map',      'product/ProductPlaceholder', 'perf.skill_map:view',      'NodeIndexOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9507, 1, 9505, 'page', 'perf_pipeline_build', '梯队建设', '/performance-talent/pipeline/build',        'product/ProductPlaceholder', 'perf.pipeline_build:view', 'RiseOutlined',      2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9508, 1, 9505, 'page', 'perf_talent_map',     '人才地图', '/performance-talent/pipeline/talent-map',   'product/ProductPlaceholder', 'perf.talent_map:view',     'GlobalOutlined',    3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9750, 1, 9500, 'perf:view',                    '查看绩效人才', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9751, 1, 9501, 'perf.work_assess:view',        '查看工作考核', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9752, 1, 9502, 'perf.assess_structure:view',   '查看结构考核', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9753, 1, 9503, 'perf.assess_qualification:view','查看资质考核', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9754, 1, 9504, 'perf.assess_resource:view',    '查看资源考核', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9755, 1, 9505, 'perf.talent_pipeline:view',    '查看人才梯队', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9756, 1, 9506, 'perf.skill_map:view',          '查看技能图谱', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9757, 1, 9507, 'perf.pipeline_build:view',     '查看梯队建设', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9758, 1, 9508, 'perf.talent_map:view',         '查看人才地图', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 95000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE id BETWEEN 9500 AND 9508 AND tenant_id = 1 AND deleted = 0;

-- 6. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 97500 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE id BETWEEN 9750 AND 9758 AND tenant_id = 1 AND deleted = 0;
