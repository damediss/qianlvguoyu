-- ============================================================
-- 运营流程 → 培训运营流程 子菜单
-- 将 6 个父级菜单改为 directory，并添加子页面
-- ============================================================

-- 1. 父级菜单改为 directory
UPDATE sys_menu
SET menu_type = 'directory', component = NULL, updated_at = NOW(), updated_by = 1
WHERE id IN (9051, 9052, 9053, 9054, 9057, 9058) AND tenant_id = 1 AND deleted = 0;

-- 2. 需求调研子菜单 (parent_id=9051)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9060, 1, 9051, 'page', 'ops_talent_demand',   '人才需求', '/ops-flow/training-ops/demand-survey/talent',   'product/ProductPlaceholder', 'ops.talent_demand:view',   'TeamOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9061, 1, 9051, 'page', 'ops_training_demand', '培训需求', '/ops-flow/training-ops/demand-survey/training', 'product/ProductPlaceholder', 'ops.training_demand:view', 'BookOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 方案设计子菜单 (parent_id=9052)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9062, 1, 9052, 'page', 'ops_class_split_setting',      '分班设置',           '/ops-flow/training-ops/solution-design/class-split',      'product/ProductPlaceholder', 'ops.class_split_setting:view',      'ApartmentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9063, 1, 9052, 'page', 'ops_schedule_setting',         '排课（计划）设置',   '/ops-flow/training-ops/solution-design/schedule',         'product/ProductPlaceholder', 'ops.schedule_setting:view',         'CalendarOutlined',    2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9064, 1, 9052, 'page', 'ops_incentive_setting',        '激励设置',           '/ops-flow/training-ops/solution-design/incentive',        'product/ProductPlaceholder', 'ops.incentive_setting:view',        'GiftOutlined',        3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9065, 1, 9052, 'page', 'ops_budget_setting',           '经费设置',           '/ops-flow/training-ops/solution-design/budget',           'product/ProductPlaceholder', 'ops.budget_setting:view',           'DollarOutlined',      4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9066, 1, 9052, 'page', 'ops_ai_learning_task_setting', 'AI学习任务设置',     '/ops-flow/training-ops/solution-design/ai-learning-task', 'product/ProductPlaceholder', 'ops.ai_learning_task_setting:view', 'RobotOutlined',       5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9067, 1, 9052, 'page', 'ops_academic_division_setting','教务分工设置',       '/ops-flow/training-ops/solution-design/academic-division', 'product/ProductPlaceholder', 'ops.academic_division_setting:view','UserSwitchOutlined',  6, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 开班管理子菜单 (parent_id=9053)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9068, 1, 9053, 'page', 'ops_material_generate', '材料生成', '/ops-flow/training-ops/class-mgmt/material-generate', 'product/ProductPlaceholder', 'ops.material_generate:view', 'FileAddOutlined',   1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9069, 1, 9053, 'page', 'ops_material_apply',    '材料申报', '/ops-flow/training-ops/class-mgmt/material-apply',    'product/ProductPlaceholder', 'ops.material_apply:view',    'FormOutlined',      2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9070, 1, 9053, 'page', 'ops_mobilization_meeting','动员会', '/ops-flow/training-ops/class-mgmt/mobilization',      'product/ProductPlaceholder', 'ops.mobilization_meeting:view','SoundOutlined',   3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 培训实施子菜单 (parent_id=9054)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9071, 1, 9054, 'page', 'ops_attendance_mgmt',  '出勤率管理', '/ops-flow/training-ops/implementation/attendance',  'product/ProductPlaceholder', 'ops.attendance_mgmt:view',  'CheckCircleOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9072, 1, 9054, 'page', 'ops_pass_rate_mgmt',   '通过率管理', '/ops-flow/training-ops/implementation/pass-rate',   'product/ProductPlaceholder', 'ops.pass_rate_mgmt:view',   'LineChartOutlined',   2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9073, 1, 9054, 'page', 'ops_homework_mgmt',    '作业管理',   '/ops-flow/training-ops/implementation/homework',    'product/ProductPlaceholder', 'ops.homework_mgmt:view',    'EditOutlined',        3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9074, 1, 9054, 'page', 'ops_evaluation_mgmt',  '评价管理',   '/ops-flow/training-ops/implementation/evaluation',  'product/ProductPlaceholder', 'ops.evaluation_mgmt:view',  'StarOutlined',        4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9075, 1, 9054, 'page', 'ops_record_mgmt',      '记录管理',   '/ops-flow/training-ops/implementation/record',      'product/ProductPlaceholder', 'ops.record_mgmt:view',      'HistoryOutlined',     5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 项目结题子菜单 (parent_id=9057)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9076, 1, 9057, 'page', 'ops_subsidy_material_apply',  '补贴申报材料申报', '/ops-flow/training-ops/project-close/subsidy-apply',  'product/ProductPlaceholder', 'ops.subsidy_material_apply:view',  'FileProtectOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9077, 1, 9057, 'page', 'ops_subsidy_material_export', '补贴申报材料导出', '/ops-flow/training-ops/project-close/subsidy-export', 'product/ProductPlaceholder', 'ops.subsidy_material_export:view', 'ExportOutlined',      2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 7. 项目申报子菜单 (parent_id=9058)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9078, 1, 9058, 'page', 'ops_qianlv_reward_apply',    '千铝奖励申报',       '/ops-flow/training-ops/project-apply/qianlv-reward',    'product/ProductPlaceholder', 'ops.qianlv_reward_apply:view',    'TrophyOutlined',        1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9079, 1, 9058, 'page', 'ops_gov_reward_guide',       '政府奖励申报指引',   '/ops-flow/training-ops/project-apply/gov-reward',       'product/ProductPlaceholder', 'ops.gov_reward_guide:view',       'BankOutlined',          2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9080, 1, 9058, 'page', 'ops_enterprise_reward_apply','企业奖励申报',       '/ops-flow/training-ops/project-apply/enterprise-reward', 'product/ProductPlaceholder', 'ops.enterprise_reward_apply:view','ShopOutlined',          3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9081, 1, 9058, 'page', 'ops_college_reward_apply',   '院校奖励申报',       '/ops-flow/training-ops/project-apply/college-reward',   'product/ProductPlaceholder', 'ops.college_reward_apply:view',   'ReadOutlined',          4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9082, 1, 9058, 'page', 'ops_student_service_flow',   '学员服务流程',       '/ops-flow/training-ops/project-apply/student-service',  'product/ProductPlaceholder', 'ops.student_service_flow:view',   'CustomerServiceOutlined',5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9083, 1, 9058, 'page', 'ops_title_apply_flow',       '职称申报流程',       '/ops-flow/training-ops/project-apply/title-apply',      'product/ProductPlaceholder', 'ops.title_apply_flow:view',       'IdcardOutlined',        6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9084, 1, 9058, 'page', 'ops_subsidy_apply_flow',     '补贴申报流程',       '/ops-flow/training-ops/project-apply/subsidy-apply',    'product/ProductPlaceholder', 'ops.subsidy_apply_flow:view',     'AccountBookOutlined',   7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9085, 1, 9058, 'page', 'ops_points_exchange_flow',   '积分兑换流程',       '/ops-flow/training-ops/project-apply/points-exchange',  'product/ProductPlaceholder', 'ops.points_exchange_flow:view',   'GiftOutlined',          8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9086, 1, 9058, 'page', 'ops_expert_apply_flow',      '专家申请流程',       '/ops-flow/training-ops/project-apply/expert-apply',     'product/ProductPlaceholder', 'ops.expert_apply_flow:view',      'UserSwitchOutlined',    9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9087, 1, 9058, 'page', 'ops_honor_apply_flow',       '荣誉申报流程',       '/ops-flow/training-ops/project-apply/honor-apply',      'product/ProductPlaceholder', 'ops.honor_apply_flow:view',       'SafetyCertificateOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 8. 权限 (28 条)
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9160, 1, 9060, 'ops.talent_demand:view',            '查看人才需求',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9161, 1, 9061, 'ops.training_demand:view',          '查看培训需求',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9162, 1, 9062, 'ops.class_split_setting:view',      '查看分班设置',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9163, 1, 9063, 'ops.schedule_setting:view',         '查看排课（计划）设置',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9164, 1, 9064, 'ops.incentive_setting:view',        '查看激励设置',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9165, 1, 9065, 'ops.budget_setting:view',           '查看经费设置',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9166, 1, 9066, 'ops.ai_learning_task_setting:view', '查看AI学习任务设置',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9167, 1, 9067, 'ops.academic_division_setting:view','查看教务分工设置',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9168, 1, 9068, 'ops.material_generate:view',        '查看材料生成',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9169, 1, 9069, 'ops.material_apply:view',           '查看材料申报',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9170, 1, 9070, 'ops.mobilization_meeting:view',     '查看动员会',             'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9171, 1, 9071, 'ops.attendance_mgmt:view',          '查看出勤率管理',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9172, 1, 9072, 'ops.pass_rate_mgmt:view',           '查看通过率管理',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9173, 1, 9073, 'ops.homework_mgmt:view',            '查看作业管理',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9174, 1, 9074, 'ops.evaluation_mgmt:view',          '查看评价管理',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9175, 1, 9075, 'ops.record_mgmt:view',              '查看记录管理',           'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9176, 1, 9076, 'ops.subsidy_material_apply:view',   '查看补贴申报材料申报',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9177, 1, 9077, 'ops.subsidy_material_export:view',  '查看补贴申报材料导出',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9178, 1, 9078, 'ops.qianlv_reward_apply:view',      '查看千铝奖励申报',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9179, 1, 9079, 'ops.gov_reward_guide:view',         '查看政府奖励申报指引',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9180, 1, 9080, 'ops.enterprise_reward_apply:view',  '查看企业奖励申报',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9181, 1, 9081, 'ops.college_reward_apply:view',     '查看院校奖励申报',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9182, 1, 9082, 'ops.student_service_flow:view',     '查看学员服务流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9183, 1, 9083, 'ops.title_apply_flow:view',         '查看职称申报流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9184, 1, 9084, 'ops.subsidy_apply_flow:view',       '查看补贴申报流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9185, 1, 9085, 'ops.points_exchange_flow:view',     '查看积分兑换流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9186, 1, 9086, 'ops.expert_apply_flow:view',        '查看专家申请流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9187, 1, 9087, 'ops.honor_apply_flow:view',         '查看荣誉申报流程',       'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 9. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9060 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9061, 1 UNION ALL SELECT 9062, 1 UNION ALL SELECT 9063, 1 UNION ALL
  SELECT 9064, 1 UNION ALL SELECT 9065, 1 UNION ALL SELECT 9066, 1 UNION ALL
  SELECT 9067, 1 UNION ALL SELECT 9068, 1 UNION ALL SELECT 9069, 1 UNION ALL
  SELECT 9070, 1 UNION ALL SELECT 9071, 1 UNION ALL SELECT 9072, 1 UNION ALL
  SELECT 9073, 1 UNION ALL SELECT 9074, 1 UNION ALL SELECT 9075, 1 UNION ALL
  SELECT 9076, 1 UNION ALL SELECT 9077, 1 UNION ALL SELECT 9078, 1 UNION ALL
  SELECT 9079, 1 UNION ALL SELECT 9080, 1 UNION ALL SELECT 9081, 1 UNION ALL
  SELECT 9082, 1 UNION ALL SELECT 9083, 1 UNION ALL SELECT 9084, 1 UNION ALL
  SELECT 9085, 1 UNION ALL SELECT 9086, 1 UNION ALL SELECT 9087, 1
) t;

-- 10. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9160 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9161, 1 UNION ALL SELECT 9162, 1 UNION ALL SELECT 9163, 1 UNION ALL
  SELECT 9164, 1 UNION ALL SELECT 9165, 1 UNION ALL SELECT 9166, 1 UNION ALL
  SELECT 9167, 1 UNION ALL SELECT 9168, 1 UNION ALL SELECT 9169, 1 UNION ALL
  SELECT 9170, 1 UNION ALL SELECT 9171, 1 UNION ALL SELECT 9172, 1 UNION ALL
  SELECT 9173, 1 UNION ALL SELECT 9174, 1 UNION ALL SELECT 9175, 1 UNION ALL
  SELECT 9176, 1 UNION ALL SELECT 9177, 1 UNION ALL SELECT 9178, 1 UNION ALL
  SELECT 9179, 1 UNION ALL SELECT 9180, 1 UNION ALL SELECT 9181, 1 UNION ALL
  SELECT 9182, 1 UNION ALL SELECT 9183, 1 UNION ALL SELECT 9184, 1 UNION ALL
  SELECT 9185, 1 UNION ALL SELECT 9186, 1 UNION ALL SELECT 9187, 1
) t;