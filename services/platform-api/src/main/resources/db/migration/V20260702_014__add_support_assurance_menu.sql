-- ============================================================
-- 支持保障 顶层菜单树
-- 注：原文档第二项「政策库」按子菜单内容实为「专家库」
-- ============================================================

-- 幂等清理：若上次迁移在权限步骤失败，先清除已插入的数据
DELETE FROM sys_role_permission WHERE permission_id BETWEEN 9400 AND 9447 AND tenant_id = 1;
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 9300 AND 9347 AND tenant_id = 1;
DELETE FROM sys_permission WHERE id BETWEEN 9400 AND 9447 AND tenant_id = 1;
DELETE FROM sys_menu WHERE id BETWEEN 9300 AND 9347 AND tenant_id = 1;

-- 1. 支持保障 顶层目录
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9300, 1, 0, 'directory', 'support_assurance', '支持保障', '/support', NULL, 'support:view', 'SafetyOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 政策库
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9301, 1, 9300, 'directory', 'sup_policy_lib', '政策库', '/support/policy', NULL, 'support.policy:view', 'FileProtectOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9302, 1, 9301, 'page', 'sup_policy_national',  '国家政策', '/support/policy/national',  'product/ProductPlaceholder', 'support.policy_national:view',  'BankOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9303, 1, 9301, 'page', 'sup_policy_provincial','省级政策', '/support/policy/provincial','product/ProductPlaceholder', 'support.policy_provincial:view','GlobalOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9304, 1, 9301, 'page', 'sup_policy_city',      '地市政策', '/support/policy/city',      'product/ProductPlaceholder', 'support.policy_city:view',      'EnvironmentOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9305, 1, 9301, 'page', 'sup_policy_company',   '公司政策', '/support/policy/company',   'product/ProductPlaceholder', 'support.policy_company:view',   'ShopOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 专家库（原文档第二项「政策库」）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9306, 1, 9300, 'directory', 'sup_expert_lib', '专家库', '/support/expert', NULL, 'support.expert:view', 'UserSwitchOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9307, 1, 9306, 'page', 'sup_expert_question', '题库专家管理', '/support/expert/question', 'product/ProductPlaceholder', 'support.expert_question:view', 'DatabaseOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9308, 1, 9306, 'page', 'sup_trainer_mgmt',    '培训师管理',   '/support/expert/trainer',  'product/ProductPlaceholder', 'support.trainer_mgmt:view',    'TeamOutlined',     2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9309, 1, 9306, 'page', 'sup_assessor_mgmt',   '考评员管理',   '/support/expert/assessor', 'product/ProductPlaceholder', 'support.assessor_mgmt:view',   'SafetyCertificateOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9310, 1, 9306, 'page', 'sup_referee_mgmt',    '裁判管理',     '/support/expert/referee',  'product/ProductPlaceholder', 'support.referee_mgmt:view',    'TrophyOutlined',   4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9311, 1, 9306, 'page', 'sup_academic_staff',  '教务人员管理', '/support/expert/academic', 'product/ProductPlaceholder', 'support.academic_staff:view',  'ReadOutlined',     5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9312, 1, 9306, 'page', 'sup_exam_staff',      '考务人员管理', '/support/expert/exam',     'product/ProductPlaceholder', 'support.exam_staff:view',      'ScheduleOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9313, 1, 9306, 'page', 'sup_comp_staff',      '赛务人员管理', '/support/expert/comp',     'product/ProductPlaceholder', 'support.comp_staff:view',      'FlagOutlined',     7, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. 设备库
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9314, 1, 9300, 'directory', 'sup_equipment_lib', '设备库', '/support/equipment', NULL, 'support.equipment:view', 'ToolOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9315, 1, 9314, 'page', 'sup_equip_practical', '实训设备', '/support/equipment/practical', 'product/ProductPlaceholder', 'support.equip_practical:view', 'ExperimentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9316, 1, 9314, 'page', 'sup_equip_training',  '培训设备', '/support/equipment/training',  'product/ProductPlaceholder', 'support.equip_training:view',  'BookOutlined',       2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9317, 1, 9314, 'page', 'sup_equip_cert',      '鉴定设备', '/support/equipment/cert',      'product/ProductPlaceholder', 'support.equip_cert:view',      'SafetyCertificateOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9318, 1, 9314, 'page', 'sup_equip_comp',      '竞赛设备', '/support/equipment/comp',      'product/ProductPlaceholder', 'support.equip_comp:view',      'TrophyOutlined',   4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 5. 物料库
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9319, 1, 9300, 'directory', 'sup_material_lib', '物料库', '/support/material', NULL, 'support.material:view', 'InboxOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9320, 1, 9319, 'page', 'sup_job_mgmt',        '职业管理', '/support/material/job',        'product/ProductPlaceholder', 'support.job_mgmt:view',        'IdcardOutlined',  1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9321, 1, 9319, 'page', 'sup_post_mgmt',       '岗位管理', '/support/material/post',       'product/ProductPlaceholder', 'support.post_mgmt:view',       'ApartmentOutlined',2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9322, 1, 9319, 'page', 'sup_standard_mgmt',   '标准管理', '/support/material/standard',   'product/ProductPlaceholder', 'support.standard_mgmt:view',   'FileTextOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9323, 1, 9319, 'page', 'sup_question_bank',   '题库管理', '/support/material/question',   'product/ProductPlaceholder', 'support.question_bank:view',   'DatabaseOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9324, 1, 9319, 'page', 'sup_textbook_mgmt',   '教材管理', '/support/material/textbook',   'product/ProductPlaceholder', 'support.textbook_mgmt:view',   'BookOutlined',     5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9325, 1, 9319, 'page', 'sup_video_mgmt',      '视频管理', '/support/material/video',      'product/ProductPlaceholder', 'support.video_mgmt:view',      'PlayCircleOutlined',6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9326, 1, 9319, 'page', 'sup_material_mgmt',   '物料管理', '/support/material/item',       'product/ProductPlaceholder', 'support.material_mgmt:view',   'InboxOutlined',    7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9327, 1, 9319, 'page', 'sup_ad_mgmt',         '广告管理', '/support/material/ad',         'product/ProductPlaceholder', 'support.ad_mgmt:view',         'NotificationOutlined',8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9328, 1, 9319, 'page', 'sup_logo_mgmt',       '标识管理', '/support/material/logo',       'product/ProductPlaceholder', 'support.logo_mgmt:view',       'TagOutlined',      9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9329, 1, 9319, 'page', 'sup_budget_mgmt',     '经费管理', '/support/material/budget',     'product/ProductPlaceholder', 'support.budget_mgmt:view',     'DollarOutlined',   10, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 6. 方法库
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9330, 1, 9300, 'directory', 'sup_method_lib', '方法库', '/support/method', NULL, 'support.method:view', 'BulbOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9331, 1, 9330, 'page', 'sup_rule_standard',  '标准规则',         '/support/method/standard',  'product/ProductPlaceholder', 'support.rule_standard:view',  'FileTextOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9332, 1, 9330, 'page', 'sup_rule_question',  '题库规则',         '/support/method/question',  'product/ProductPlaceholder', 'support.rule_question:view',  'DatabaseOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9333, 1, 9330, 'page', 'sup_rule_textbook',  '教材规则',         '/support/method/textbook',  'product/ProductPlaceholder', 'support.rule_textbook:view',  'BookOutlined',     3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9334, 1, 9330, 'page', 'sup_rule_video',     '视频规则',         '/support/method/video',     'product/ProductPlaceholder', 'support.rule_video:view',     'PlayCircleOutlined',4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9335, 1, 9330, 'page', 'sup_rule_practical', '实训规则',         '/support/method/practical', 'product/ProductPlaceholder', 'support.rule_practical:view', 'ExperimentOutlined',5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9336, 1, 9330, 'page', 'sup_rule_forum',     '论坛规则',         '/support/method/forum',     'product/ProductPlaceholder', 'support.rule_forum:view',     'CommentOutlined',  6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9337, 1, 9330, 'page', 'sup_rule_training',  '培训规则',         '/support/method/training',  'product/ProductPlaceholder', 'support.rule_training:view',  'ScheduleOutlined', 7, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9338, 1, 9330, 'page', 'sup_rule_cert',      '认定规则',         '/support/method/cert',      'product/ProductPlaceholder', 'support.rule_cert:view',      'SafetyCertificateOutlined', 8, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9339, 1, 9330, 'page', 'sup_rule_comp',      '竞赛规则',         '/support/method/comp',      'product/ProductPlaceholder', 'support.rule_comp:view',      'TrophyOutlined',   9, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9340, 1, 9330, 'page', 'sup_rule_ceremony',  '开闭幕仪式规则',   '/support/method/ceremony',  'product/ProductPlaceholder', 'support.rule_ceremony:view',  'FlagOutlined',     10, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 7. 环境库
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9341, 1, 9300, 'directory', 'sup_env_lib', '环境库', '/support/environment', NULL, 'support.env:view', 'HomeOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9342, 1, 9341, 'page', 'sup_env_forum',           '论坛布置标准',           '/support/environment/forum',      'product/ProductPlaceholder', 'support.env_forum:view',           'CommentOutlined',  1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9343, 1, 9341, 'page', 'sup_env_question_accept', '题库验收布置标准',       '/support/environment/question',   'product/ProductPlaceholder', 'support.env_question_accept:view', 'DatabaseOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9344, 1, 9341, 'page', 'sup_env_ceremony',        '开（闭）幕仪式布置标准', '/support/environment/ceremony',   'product/ProductPlaceholder', 'support.env_ceremony:view',        'FlagOutlined',     3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9345, 1, 9341, 'page', 'sup_env_training_class',  '培训班布置标准',         '/support/environment/training',   'product/ProductPlaceholder', 'support.env_training_class:view',  'BookOutlined',     4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9346, 1, 9341, 'page', 'sup_env_cert_exam',       '认定考场布置标准',       '/support/environment/cert-exam',  'product/ProductPlaceholder', 'support.env_cert_exam:view',       'SafetyCertificateOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (9347, 1, 9341, 'page', 'sup_env_comp_site',       '竞赛现场布置标准',       '/support/environment/comp-site',  'product/ProductPlaceholder', 'support.env_comp_site:view',       'TrophyOutlined',   6, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 8. 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9400, 1, 9300, 'support:view',                    '查看支持保障',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9401, 1, 9301, 'support.policy:view',             '查看政策库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9402, 1, 9302, 'support.policy_national:view',    '查看国家政策',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9403, 1, 9303, 'support.policy_provincial:view',  '查看省级政策',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9404, 1, 9304, 'support.policy_city:view',        '查看地市政策',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9405, 1, 9305, 'support.policy_company:view',     '查看公司政策',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9406, 1, 9306, 'support.expert:view',             '查看专家库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9407, 1, 9307, 'support.expert_question:view',    '查看题库专家管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9408, 1, 9308, 'support.trainer_mgmt:view',       '查看培训师管理',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9409, 1, 9309, 'support.assessor_mgmt:view',      '查看考评员管理',   'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9410, 1, 9310, 'support.referee_mgmt:view',       '查看裁判管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9411, 1, 9311, 'support.academic_staff:view',     '查看教务人员管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9412, 1, 9312, 'support.exam_staff:view',         '查看考务人员管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9413, 1, 9313, 'support.comp_staff:view',         '查看赛务人员管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9414, 1, 9314, 'support.equipment:view',          '查看设备库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9415, 1, 9315, 'support.equip_practical:view',    '查看实训设备',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9416, 1, 9316, 'support.equip_training:view',     '查看培训设备',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9417, 1, 9317, 'support.equip_cert:view',         '查看鉴定设备',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9418, 1, 9318, 'support.equip_comp:view',         '查看竞赛设备',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9419, 1, 9319, 'support.material:view',           '查看物料库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9420, 1, 9320, 'support.job_mgmt:view',           '查看职业管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9421, 1, 9321, 'support.post_mgmt:view',          '查看岗位管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9422, 1, 9322, 'support.standard_mgmt:view',      '查看标准管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9423, 1, 9323, 'support.question_bank:view',      '查看题库管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9424, 1, 9324, 'support.textbook_mgmt:view',      '查看教材管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9425, 1, 9325, 'support.video_mgmt:view',         '查看视频管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9426, 1, 9326, 'support.material_mgmt:view',      '查看物料管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9427, 1, 9327, 'support.ad_mgmt:view',            '查看广告管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9428, 1, 9328, 'support.logo_mgmt:view',          '查看标识管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9429, 1, 9329, 'support.budget_mgmt:view',        '查看经费管理',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9430, 1, 9330, 'support.method:view',             '查看方法库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9431, 1, 9331, 'support.rule_standard:view',      '查看标准规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9432, 1, 9332, 'support.rule_question:view',      '查看题库规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9433, 1, 9333, 'support.rule_textbook:view',      '查看教材规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9434, 1, 9334, 'support.rule_video:view',         '查看视频规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9435, 1, 9335, 'support.rule_practical:view',     '查看实训规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9436, 1, 9336, 'support.rule_forum:view',         '查看论坛规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9437, 1, 9337, 'support.rule_training:view',      '查看培训规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9438, 1, 9338, 'support.rule_cert:view',          '查看认定规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9439, 1, 9339, 'support.rule_comp:view',          '查看竞赛规则',     'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9440, 1, 9340, 'support.rule_ceremony:view',      '查看开闭幕仪式规则', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9441, 1, 9341, 'support.env:view',                '查看环境库',       'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9442, 1, 9342, 'support.env_forum:view',          '查看论坛布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9443, 1, 9343, 'support.env_question_accept:view','查看题库验收布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9444, 1, 9344, 'support.env_ceremony:view',       '查看开（闭）幕仪式布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9445, 1, 9345, 'support.env_training_class:view', '查看培训班布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9446, 1, 9346, 'support.env_cert_exam:view',      '查看认定考场布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9447, 1, 9347, 'support.env_comp_site:view',      '查看竞赛现场布置标准', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 9. 角色-菜单关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 93000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE id BETWEEN 9300 AND 9347 AND tenant_id = 1 AND deleted = 0;

-- 10. 角色-权限关联 (平台管理员 role_id=2000)
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 94000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE id BETWEEN 9400 AND 9447 AND tenant_id = 1 AND deleted = 0;
