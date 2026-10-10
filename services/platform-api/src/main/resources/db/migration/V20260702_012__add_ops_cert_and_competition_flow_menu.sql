-- ============================================================
-- 运营流程 → 认定运营流程 / 竞赛运营流程
-- 与业务销售流程（9001）同级，挂在运营流程（9000）下
-- ============================================================

-- 1. 认定运营流程 目录 (parent_id=9000, sort_order=4)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9090, 1, 9000, 'directory', 'ops_cert_ops_flow', '认定运营流程', '/ops-flow/cert-ops', NULL, 'ops.cert_ops_flow:view', 'SafetyCertificateOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 认定运营流程子菜单 (parent_id=9090)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9091, 1, 9090, 'page', 'ops_cert_notice_mgmt',    '通知管理', '/ops-flow/cert-ops/notice',       'product/ProductPlaceholder', 'ops.cert_notice_mgmt:view',    'BellOutlined',              1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9092, 1, 9090, 'page', 'ops_cert_qualification',  '资格审核', '/ops-flow/cert-ops/qualification', 'product/ProductPlaceholder', 'ops.cert_qualification:view',  'AuditOutlined',             2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9093, 1, 9090, 'page', 'ops_cert_plan_mgmt',      '计划管理', '/ops-flow/cert-ops/plan',         'product/ProductPlaceholder', 'ops.cert_plan_mgmt:view',      'CalendarOutlined',          3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9094, 1, 9090, 'page', 'ops_cert_exam_arrange',   '考场编排', '/ops-flow/cert-ops/exam-arrange', 'product/ProductPlaceholder', 'ops.cert_exam_arrange:view',   'ApartmentOutlined',         4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9095, 1, 9090, 'page', 'ops_cert_pre_exam_prep',  '考前准备', '/ops-flow/cert-ops/pre-exam-prep','product/ProductPlaceholder', 'ops.cert_pre_exam_prep:view',  'FileProtectOutlined',       5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9096, 1, 9090, 'page', 'ops_cert_exam_meeting',   '考务会议', '/ops-flow/cert-ops/exam-meeting', 'product/ProductPlaceholder', 'ops.cert_exam_meeting:view',   'TeamOutlined',              6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9097, 1, 9090, 'page', 'ops_cert_assessment',     '考评进行', '/ops-flow/cert-ops/assessment',   'product/ProductPlaceholder', 'ops.cert_assessment:view',     'CheckCircleOutlined',       7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9098, 1, 9090, 'page', 'ops_cert_score_publish',  '成绩公示', '/ops-flow/cert-ops/score-publish','product/ProductPlaceholder', 'ops.cert_score_publish:view',  'FundOutlined',              8, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 竞赛运营流程 目录 (parent_id=9000, sort_order=5)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9100, 1, 9000, 'directory', 'ops_competition_ops_flow', '竞赛运营流程', '/ops-flow/competition-ops', NULL, 'ops.competition_ops_flow:view', 'TrophyOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 竞赛运营流程子菜单 (parent_id=9100)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9101, 1, 9100, 'page', 'ops_comp_apply',           '竞赛申报',       '/ops-flow/competition-ops/apply',           'product/ProductPlaceholder', 'ops.comp_apply:view',           'FormOutlined',        1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9102, 1, 9100, 'page', 'ops_comp_register',        '竞赛报名',       '/ops-flow/competition-ops/register',        'product/ProductPlaceholder', 'ops.comp_register:view',        'UsergroupAddOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9103, 1, 9100, 'page', 'ops_comp_tech_seminar',    '技术研讨',       '/ops-flow/competition-ops/tech-seminar',    'product/ProductPlaceholder', 'ops.comp_tech_seminar:view',    'BulbOutlined',        3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9104, 1, 9100, 'page', 'ops_comp_enterprise_sel',  '企业选拔赛',     '/ops-flow/competition-ops/enterprise-sel',  'product/ProductPlaceholder', 'ops.comp_enterprise_sel:view',  'ShopOutlined',        4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9105, 1, 9100, 'page', 'ops_comp_preliminary',     '初赛（地市赛）', '/ops-flow/competition-ops/preliminary',     'product/ProductPlaceholder', 'ops.comp_preliminary:view',     'FlagOutlined',        5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9106, 1, 9100, 'page', 'ops_comp_semifinal',       '复赛',           '/ops-flow/competition-ops/semifinal',       'product/ProductPlaceholder', 'ops.comp_semifinal:view',       'RiseOutlined',        6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9107, 1, 9100, 'page', 'ops_comp_pre_training',    '赛前培训',       '/ops-flow/competition-ops/pre-training',    'product/ProductPlaceholder', 'ops.comp_pre_training:view',    'BookOutlined',        7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9108, 1, 9100, 'page', 'ops_comp_opening',         '开幕仪式',       '/ops-flow/competition-ops/opening',         'product/ProductPlaceholder', 'ops.comp_opening:view',         'PlayCircleOutlined',  8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9109, 1, 9100, 'page', 'ops_comp_final',           '决赛',           '/ops-flow/competition-ops/final',           'product/ProductPlaceholder', 'ops.comp_final:view',           'TrophyOutlined',      9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9110, 1, 9100, 'page', 'ops_comp_closing',         '闭幕仪式',       '/ops-flow/competition-ops/closing',         'product/ProductPlaceholder', 'ops.comp_closing:view',         'CheckCircleOutlined', 10, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9111, 1, 9100, 'page', 'ops_comp_project_close',   '竞赛结题',       '/ops-flow/competition-ops/project-close',   'product/ProductPlaceholder', 'ops.comp_project_close:view',   'FileTextOutlined',    11, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 权限 (21 条)
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9190, 1, 9090, 'ops.cert_ops_flow:view',        '查看认定运营流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9191, 1, 9091, 'ops.cert_notice_mgmt:view',     '查看通知管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9192, 1, 9092, 'ops.cert_qualification:view',   '查看资格审核',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9193, 1, 9093, 'ops.cert_plan_mgmt:view',       '查看计划管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9194, 1, 9094, 'ops.cert_exam_arrange:view',    '查看考场编排',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9195, 1, 9095, 'ops.cert_pre_exam_prep:view',   '查看考前准备',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9196, 1, 9096, 'ops.cert_exam_meeting:view',    '查看考务会议',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9197, 1, 9097, 'ops.cert_assessment:view',      '查看考评进行',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9198, 1, 9098, 'ops.cert_score_publish:view',   '查看成绩公示',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9199, 1, 9100, 'ops.competition_ops_flow:view', '查看竞赛运营流程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9200, 1, 9101, 'ops.comp_apply:view',           '查看竞赛申报',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9201, 1, 9102, 'ops.comp_register:view',        '查看竞赛报名',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9202, 1, 9103, 'ops.comp_tech_seminar:view',    '查看技术研讨',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9203, 1, 9104, 'ops.comp_enterprise_sel:view',  '查看企业选拔赛',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9204, 1, 9105, 'ops.comp_preliminary:view',     '查看初赛（地市赛）','view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9205, 1, 9106, 'ops.comp_semifinal:view',       '查看复赛',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9206, 1, 9107, 'ops.comp_pre_training:view',    '查看赛前培训',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9207, 1, 9108, 'ops.comp_opening:view',         '查看开幕仪式',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9208, 1, 9109, 'ops.comp_final:view',           '查看决赛',         'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9209, 1, 9110, 'ops.comp_closing:view',         '查看闭幕仪式',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9210, 1, 9111, 'ops.comp_project_close:view',   '查看竞赛结题',     'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 90000 + menu_id, tenant_id, 2000, menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9090 AS menu_id, 1 AS tenant_id UNION ALL
  SELECT 9091, 1 UNION ALL SELECT 9092, 1 UNION ALL SELECT 9093, 1 UNION ALL
  SELECT 9094, 1 UNION ALL SELECT 9095, 1 UNION ALL SELECT 9096, 1 UNION ALL
  SELECT 9097, 1 UNION ALL SELECT 9098, 1 UNION ALL SELECT 9100, 1 UNION ALL
  SELECT 9101, 1 UNION ALL SELECT 9102, 1 UNION ALL SELECT 9103, 1 UNION ALL
  SELECT 9104, 1 UNION ALL SELECT 9105, 1 UNION ALL SELECT 9106, 1 UNION ALL
  SELECT 9107, 1 UNION ALL SELECT 9108, 1 UNION ALL SELECT 9109, 1 UNION ALL
  SELECT 9110, 1 UNION ALL SELECT 9111, 1
) t;

-- 7. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 91000 + permission_id, tenant_id, 2000, permission_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT 9190 AS permission_id, 1 AS tenant_id UNION ALL
  SELECT 9191, 1 UNION ALL SELECT 9192, 1 UNION ALL SELECT 9193, 1 UNION ALL
  SELECT 9194, 1 UNION ALL SELECT 9195, 1 UNION ALL SELECT 9196, 1 UNION ALL
  SELECT 9197, 1 UNION ALL SELECT 9198, 1 UNION ALL SELECT 9199, 1 UNION ALL
  SELECT 9200, 1 UNION ALL SELECT 9201, 1 UNION ALL SELECT 9202, 1 UNION ALL
  SELECT 9203, 1 UNION ALL SELECT 9204, 1 UNION ALL SELECT 9205, 1 UNION ALL
  SELECT 9206, 1 UNION ALL SELECT 9207, 1 UNION ALL SELECT 9208, 1 UNION ALL
  SELECT 9209, 1 UNION ALL SELECT 9210, 1
) t;
