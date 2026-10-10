-- Seed permissions for 18 new training department sub-modules (Phase 1-3 tables)
-- Menu IDs: 5001-5018, Permission IDs: 5601-5672

-- 1. Add menu items under training directory (parent_id=4300)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  -- 考试编排相关
  (5001, 1, 4300, 'page', 'train_exam-session', '考试场次', '/training/exam-sessions', 'training/ExamSession', 'train:exam-session:view', 'ClockCircleOutlined', 43, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5002, 1, 4300, 'page', 'train_exam-room', '考场管理', '/training/exam-rooms', 'training/ExamRoom', 'train:exam-room:view', 'BankOutlined', 44, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5003, 1, 4300, 'page', 'train_exam-people-room', '考生座位', '/training/exam-people-rooms', 'training/ExamPeopleRoom', 'train:exam-people-room:view', 'GroupOutlined', 45, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5004, 1, 4300, 'page', 'train_exam-staff', '考务人员', '/training/exam-staffs', 'training/ExamStaff', 'train:exam-staff:view', 'TeamOutlined', 46, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5005, 1, 4300, 'page', 'train_exam-staff-config', '考务配置', '/training/exam-staff-configs', 'training/ExamStaffConfig', 'train:exam-staff-config:view', 'SettingOutlined', 47, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5006, 1, 4300, 'page', 'train_exam-shijuan', '试卷关联', '/training/exam-shijuans', 'training/ExamShijuan', 'train:exam-shijuan:view', 'FileTextOutlined', 48, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5007, 1, 4300, 'page', 'train_exam-work-type', '考试工种', '/training/exam-work-types', 'training/ExamWorkType', 'train:exam-work-type:view', 'TagsOutlined', 49, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5008, 1, 4300, 'page', 'train_exam-review', '复习配置', '/training/exam-reviews', 'training/ExamReview', 'train:exam-review:view', 'ReadOutlined', 50, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 自主报名
  (5009, 1, 4300, 'page', 'train_sign-up', '自主报名', '/training/sign-ups', 'training/SignUp', 'train:sign-up:view', 'FormOutlined', 51, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5010, 1, 4300, 'page', 'train_sign-up-people', '报名人员', '/training/sign-up-people', 'training/SignUpPeople', 'train:sign-up-people:view', 'UserAddOutlined', 52, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5011, 1, 4300, 'page', 'train_sign-up-orc', 'OCR识别', '/training/sign-up-orcs', 'training/SignUpOrc', 'train:sign-up-orc:view', 'ScanOutlined', 53, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 文件与数据
  (5012, 1, 4300, 'page', 'train_file-temp', '文件模板', '/training/file-temps', 'training/FileTemp', 'train:file-temp:view', 'FileOutlined', 54, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5013, 1, 4300, 'page', 'train_base-data', '基础数据', '/training/base-data', 'training/BaseData', 'train:base-data:view', 'DatabaseOutlined', 55, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 岗位与需求
  (5014, 1, 4300, 'page', 'train_position-worktype', '岗位工种', '/training/position-worktypes', 'training/PositionWorktype', 'train:position-worktype:view', 'ApartmentOutlined', 56, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5015, 1, 4300, 'page', 'train_demand-train', '培训需求采集', '/training/demand-trains', 'training/DemandTrain', 'train:demand-train:view', 'ProfileOutlined', 57, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (5016, 1, 4300, 'page', 'train_demand-employee', '人才需求采集', '/training/demand-employees', 'training/DemandEmployee', 'train:demand-employee:view', 'IdcardOutlined', 58, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 考生人员
  (5017, 1, 4300, 'page', 'train_exam-people-list', '考生人员', '/training/exam-people-list', 'training/ExamPeopleList', 'train:exam-people-list:view', 'ContactsOutlined', 59, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 经费明细
  (5018, 1, 4300, 'page', 'train_solution-badge', '经费明细', '/training/solution-badges', 'training/SolutionBadge', 'train:solution-badge:view', 'DollarOutlined', 60, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. Add permissions (view/create/update/delete per module × 18 modules = 72 permissions)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  -- exam-session (menu 5001)
  (5601, 1, 5001, 'train:exam-session:view', '查看考试场次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5602, 1, 5001, 'train:exam-session:create', '新增考试场次', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5603, 1, 5001, 'train:exam-session:update', '编辑考试场次', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5604, 1, 5001, 'train:exam-session:delete', '删除考试场次', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-room (menu 5002)
  (5605, 1, 5002, 'train:exam-room:view', '查看考场', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5606, 1, 5002, 'train:exam-room:create', '新增考场', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5607, 1, 5002, 'train:exam-room:update', '编辑考场', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5608, 1, 5002, 'train:exam-room:delete', '删除考场', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-people-room (menu 5003)
  (5609, 1, 5003, 'train:exam-people-room:view', '查看考生座位', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5610, 1, 5003, 'train:exam-people-room:create', '新增考生座位', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5611, 1, 5003, 'train:exam-people-room:update', '编辑考生座位', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5612, 1, 5003, 'train:exam-people-room:delete', '删除考生座位', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-staff (menu 5004)
  (5613, 1, 5004, 'train:exam-staff:view', '查看考务人员', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5614, 1, 5004, 'train:exam-staff:create', '新增考务人员', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5615, 1, 5004, 'train:exam-staff:update', '编辑考务人员', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5616, 1, 5004, 'train:exam-staff:delete', '删除考务人员', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-staff-config (menu 5005)
  (5617, 1, 5005, 'train:exam-staff-config:view', '查看考务配置', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5618, 1, 5005, 'train:exam-staff-config:create', '新增考务配置', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5619, 1, 5005, 'train:exam-staff-config:update', '编辑考务配置', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5620, 1, 5005, 'train:exam-staff-config:delete', '删除考务配置', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-shijuan (menu 5006)
  (5621, 1, 5006, 'train:exam-shijuan:view', '查看试卷关联', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5622, 1, 5006, 'train:exam-shijuan:create', '新增试卷关联', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5623, 1, 5006, 'train:exam-shijuan:update', '编辑试卷关联', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5624, 1, 5006, 'train:exam-shijuan:delete', '删除试卷关联', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-work-type (menu 5007)
  (5625, 1, 5007, 'train:exam-work-type:view', '查看考试工种', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5626, 1, 5007, 'train:exam-work-type:create', '新增考试工种', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5627, 1, 5007, 'train:exam-work-type:update', '编辑考试工种', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5628, 1, 5007, 'train:exam-work-type:delete', '删除考试工种', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-review (menu 5008)
  (5629, 1, 5008, 'train:exam-review:view', '查看复习配置', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5630, 1, 5008, 'train:exam-review:create', '新增复习配置', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5631, 1, 5008, 'train:exam-review:update', '编辑复习配置', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5632, 1, 5008, 'train:exam-review:delete', '删除复习配置', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- sign-up (menu 5009)
  (5633, 1, 5009, 'train:sign-up:view', '查看自主报名', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5634, 1, 5009, 'train:sign-up:create', '新增自主报名', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5635, 1, 5009, 'train:sign-up:update', '编辑自主报名', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5636, 1, 5009, 'train:sign-up:delete', '删除自主报名', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- sign-up-people (menu 5010)
  (5637, 1, 5010, 'train:sign-up-people:view', '查看报名人员', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5638, 1, 5010, 'train:sign-up-people:create', '新增报名人员', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5639, 1, 5010, 'train:sign-up-people:update', '编辑报名人员', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5640, 1, 5010, 'train:sign-up-people:delete', '删除报名人员', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- sign-up-orc (menu 5011)
  (5641, 1, 5011, 'train:sign-up-orc:view', '查看OCR识别', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5642, 1, 5011, 'train:sign-up-orc:create', '新增OCR识别', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5643, 1, 5011, 'train:sign-up-orc:update', '编辑OCR识别', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5644, 1, 5011, 'train:sign-up-orc:delete', '删除OCR识别', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- file-temp (menu 5012)
  (5645, 1, 5012, 'train:file-temp:view', '查看文件模板', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5646, 1, 5012, 'train:file-temp:create', '新增文件模板', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5647, 1, 5012, 'train:file-temp:update', '编辑文件模板', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5648, 1, 5012, 'train:file-temp:delete', '删除文件模板', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- base-data (menu 5013)
  (5649, 1, 5013, 'train:base-data:view', '查看基础数据', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5650, 1, 5013, 'train:base-data:create', '新增基础数据', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5651, 1, 5013, 'train:base-data:update', '编辑基础数据', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5652, 1, 5013, 'train:base-data:delete', '删除基础数据', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- position-worktype (menu 5014)
  (5653, 1, 5014, 'train:position-worktype:view', '查看岗位工种', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5654, 1, 5014, 'train:position-worktype:create', '新增岗位工种', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5655, 1, 5014, 'train:position-worktype:update', '编辑岗位工种', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5656, 1, 5014, 'train:position-worktype:delete', '删除岗位工种', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- demand-train (menu 5015)
  (5657, 1, 5015, 'train:demand-train:view', '查看培训需求', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5658, 1, 5015, 'train:demand-train:create', '新增培训需求', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5659, 1, 5015, 'train:demand-train:update', '编辑培训需求', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5660, 1, 5015, 'train:demand-train:delete', '删除培训需求', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- demand-employee (menu 5016)
  (5661, 1, 5016, 'train:demand-employee:view', '查看人才需求', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5662, 1, 5016, 'train:demand-employee:create', '新增人才需求', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5663, 1, 5016, 'train:demand-employee:update', '编辑人才需求', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5664, 1, 5016, 'train:demand-employee:delete', '删除人才需求', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- exam-people-list (menu 5017)
  (5665, 1, 5017, 'train:exam-people-list:view', '查看考生人员', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5666, 1, 5017, 'train:exam-people-list:create', '新增考生人员', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5667, 1, 5017, 'train:exam-people-list:update', '编辑考生人员', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5668, 1, 5017, 'train:exam-people-list:delete', '删除考生人员', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- solution-badge (menu 5018)
  (5669, 1, 5018, 'train:solution-badge:view', '查看经费明细', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5670, 1, 5018, 'train:solution-badge:create', '新增经费明细', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5671, 1, 5018, 'train:solution-badge:update', '编辑经费明细', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5672, 1, 5018, 'train:solution-badge:delete', '删除经费明细', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. Grant all new menus to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604900, 1, 2000, 5001, NOW(), NOW(), 1, 1, 0),
  (604901, 1, 2000, 5002, NOW(), NOW(), 1, 1, 0),
  (604902, 1, 2000, 5003, NOW(), NOW(), 1, 1, 0),
  (604903, 1, 2000, 5004, NOW(), NOW(), 1, 1, 0),
  (604904, 1, 2000, 5005, NOW(), NOW(), 1, 1, 0),
  (604905, 1, 2000, 5006, NOW(), NOW(), 1, 1, 0),
  (604906, 1, 2000, 5007, NOW(), NOW(), 1, 1, 0),
  (604907, 1, 2000, 5008, NOW(), NOW(), 1, 1, 0),
  (604908, 1, 2000, 5009, NOW(), NOW(), 1, 1, 0),
  (604909, 1, 2000, 5010, NOW(), NOW(), 1, 1, 0),
  (604910, 1, 2000, 5011, NOW(), NOW(), 1, 1, 0),
  (604911, 1, 2000, 5012, NOW(), NOW(), 1, 1, 0),
  (604912, 1, 2000, 5013, NOW(), NOW(), 1, 1, 0),
  (604913, 1, 2000, 5014, NOW(), NOW(), 1, 1, 0),
  (604914, 1, 2000, 5015, NOW(), NOW(), 1, 1, 0),
  (604915, 1, 2000, 5016, NOW(), NOW(), 1, 1, 0),
  (604916, 1, 2000, 5017, NOW(), NOW(), 1, 1, 0),
  (604917, 1, 2000, 5018, NOW(), NOW(), 1, 1, 0);

-- 4. Grant all new permissions to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705500, 1, 2000, 5601, NOW(), NOW(), 1, 1, 0),
  (705501, 1, 2000, 5602, NOW(), NOW(), 1, 1, 0),
  (705502, 1, 2000, 5603, NOW(), NOW(), 1, 1, 0),
  (705503, 1, 2000, 5604, NOW(), NOW(), 1, 1, 0),
  (705504, 1, 2000, 5605, NOW(), NOW(), 1, 1, 0),
  (705505, 1, 2000, 5606, NOW(), NOW(), 1, 1, 0),
  (705506, 1, 2000, 5607, NOW(), NOW(), 1, 1, 0),
  (705507, 1, 2000, 5608, NOW(), NOW(), 1, 1, 0),
  (705508, 1, 2000, 5609, NOW(), NOW(), 1, 1, 0),
  (705509, 1, 2000, 5610, NOW(), NOW(), 1, 1, 0),
  (705510, 1, 2000, 5611, NOW(), NOW(), 1, 1, 0),
  (705511, 1, 2000, 5612, NOW(), NOW(), 1, 1, 0),
  (705512, 1, 2000, 5613, NOW(), NOW(), 1, 1, 0),
  (705513, 1, 2000, 5614, NOW(), NOW(), 1, 1, 0),
  (705514, 1, 2000, 5615, NOW(), NOW(), 1, 1, 0),
  (705515, 1, 2000, 5616, NOW(), NOW(), 1, 1, 0),
  (705516, 1, 2000, 5617, NOW(), NOW(), 1, 1, 0),
  (705517, 1, 2000, 5618, NOW(), NOW(), 1, 1, 0),
  (705518, 1, 2000, 5619, NOW(), NOW(), 1, 1, 0),
  (705519, 1, 2000, 5620, NOW(), NOW(), 1, 1, 0),
  (705520, 1, 2000, 5621, NOW(), NOW(), 1, 1, 0),
  (705521, 1, 2000, 5622, NOW(), NOW(), 1, 1, 0),
  (705522, 1, 2000, 5623, NOW(), NOW(), 1, 1, 0),
  (705523, 1, 2000, 5624, NOW(), NOW(), 1, 1, 0),
  (705524, 1, 2000, 5625, NOW(), NOW(), 1, 1, 0),
  (705525, 1, 2000, 5626, NOW(), NOW(), 1, 1, 0),
  (705526, 1, 2000, 5627, NOW(), NOW(), 1, 1, 0),
  (705527, 1, 2000, 5628, NOW(), NOW(), 1, 1, 0),
  (705528, 1, 2000, 5629, NOW(), NOW(), 1, 1, 0),
  (705529, 1, 2000, 5630, NOW(), NOW(), 1, 1, 0),
  (705530, 1, 2000, 5631, NOW(), NOW(), 1, 1, 0),
  (705531, 1, 2000, 5632, NOW(), NOW(), 1, 1, 0),
  (705532, 1, 2000, 5633, NOW(), NOW(), 1, 1, 0),
  (705533, 1, 2000, 5634, NOW(), NOW(), 1, 1, 0),
  (705534, 1, 2000, 5635, NOW(), NOW(), 1, 1, 0),
  (705535, 1, 2000, 5636, NOW(), NOW(), 1, 1, 0),
  (705536, 1, 2000, 5637, NOW(), NOW(), 1, 1, 0),
  (705537, 1, 2000, 5638, NOW(), NOW(), 1, 1, 0),
  (705538, 1, 2000, 5639, NOW(), NOW(), 1, 1, 0),
  (705539, 1, 2000, 5640, NOW(), NOW(), 1, 1, 0),
  (705540, 1, 2000, 5641, NOW(), NOW(), 1, 1, 0),
  (705541, 1, 2000, 5642, NOW(), NOW(), 1, 1, 0),
  (705542, 1, 2000, 5643, NOW(), NOW(), 1, 1, 0),
  (705543, 1, 2000, 5644, NOW(), NOW(), 1, 1, 0),
  (705544, 1, 2000, 5645, NOW(), NOW(), 1, 1, 0),
  (705545, 1, 2000, 5646, NOW(), NOW(), 1, 1, 0),
  (705546, 1, 2000, 5647, NOW(), NOW(), 1, 1, 0),
  (705547, 1, 2000, 5648, NOW(), NOW(), 1, 1, 0),
  (705548, 1, 2000, 5649, NOW(), NOW(), 1, 1, 0),
  (705549, 1, 2000, 5650, NOW(), NOW(), 1, 1, 0),
  (705550, 1, 2000, 5651, NOW(), NOW(), 1, 1, 0),
  (705551, 1, 2000, 5652, NOW(), NOW(), 1, 1, 0),
  (705552, 1, 2000, 5653, NOW(), NOW(), 1, 1, 0),
  (705553, 1, 2000, 5654, NOW(), NOW(), 1, 1, 0),
  (705554, 1, 2000, 5655, NOW(), NOW(), 1, 1, 0),
  (705555, 1, 2000, 5656, NOW(), NOW(), 1, 1, 0),
  (705556, 1, 2000, 5657, NOW(), NOW(), 1, 1, 0),
  (705557, 1, 2000, 5658, NOW(), NOW(), 1, 1, 0),
  (705558, 1, 2000, 5659, NOW(), NOW(), 1, 1, 0),
  (705559, 1, 2000, 5660, NOW(), NOW(), 1, 1, 0),
  (705560, 1, 2000, 5661, NOW(), NOW(), 1, 1, 0),
  (705561, 1, 2000, 5662, NOW(), NOW(), 1, 1, 0),
  (705562, 1, 2000, 5663, NOW(), NOW(), 1, 1, 0),
  (705563, 1, 2000, 5664, NOW(), NOW(), 1, 1, 0),
  (705564, 1, 2000, 5665, NOW(), NOW(), 1, 1, 0),
  (705565, 1, 2000, 5666, NOW(), NOW(), 1, 1, 0),
  (705566, 1, 2000, 5667, NOW(), NOW(), 1, 1, 0),
  (705567, 1, 2000, 5668, NOW(), NOW(), 1, 1, 0),
  (705568, 1, 2000, 5669, NOW(), NOW(), 1, 1, 0),
  (705569, 1, 2000, 5670, NOW(), NOW(), 1, 1, 0),
  (705570, 1, 2000, 5671, NOW(), NOW(), 1, 1, 0),
  (705571, 1, 2000, 5672, NOW(), NOW(), 1, 1, 0);
