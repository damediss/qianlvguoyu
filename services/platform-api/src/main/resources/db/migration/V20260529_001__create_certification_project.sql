CREATE TABLE IF NOT EXISTS certification_task (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_no VARCHAR(80) NOT NULL COMMENT '任务编号',
  task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
  task_manager_name VARCHAR(80) NOT NULL COMMENT '任务经理',
  company_name VARCHAR(200) NULL COMMENT '企业名称',
  certification_type VARCHAR(80) NULL COMMENT '认定类型',
  work_type_name VARCHAR(120) NULL COMMENT '工种',
  level_code VARCHAR(40) NULL COMMENT '等级编码',
  level_name VARCHAR(80) NULL COMMENT '等级名称',
  planned_people_count INT NOT NULL DEFAULT 0 COMMENT '计划人数',
  plan_start_date DATE NULL COMMENT '计划开始日期',
  plan_end_date DATE NULL COMMENT '计划结束日期',
  exam_date DATE NULL COMMENT '考试日期',
  source_type VARCHAR(40) NOT NULL DEFAULT 'manual' COMMENT '来源类型',
  source_name VARCHAR(120) NULL COMMENT '来源名称',
  current_phase VARCHAR(40) NOT NULL DEFAULT 'preparation' COMMENT '当前阶段',
  status VARCHAR(40) NOT NULL DEFAULT 'running' COMMENT '任务状态',
  document_completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成文档数',
  document_total_count INT NOT NULL DEFAULT 5 COMMENT '文档总数',
  phase_completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成阶段数',
  phase_total_count INT NOT NULL DEFAULT 5 COMMENT '阶段总数',
  last_business_updated_at DATETIME NULL COMMENT '最后业务更新时间',
  remark VARCHAR(1000) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_certification_task_no (tenant_id, task_no, deleted),
  KEY idx_certification_task_status (tenant_id, status, deleted),
  KEY idx_certification_task_level (tenant_id, level_code, deleted),
  KEY idx_certification_task_updated (tenant_id, last_business_updated_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目任务';

CREATE TABLE IF NOT EXISTS certification_task_phase (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  phase_code VARCHAR(40) NOT NULL COMMENT '阶段编码',
  phase_name VARCHAR(80) NOT NULL COMMENT '阶段名称',
  phase_order INT NOT NULL COMMENT '阶段顺序',
  status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '阶段状态',
  started_at DATETIME NULL COMMENT '开始时间',
  completed_at DATETIME NULL COMMENT '完成时间',
  completed_by BIGINT NULL COMMENT '完成人',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_certification_phase_task_code (tenant_id, task_id, phase_code, deleted),
  KEY idx_certification_phase_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目阶段';

CREATE TABLE IF NOT EXISTS certification_task_document (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_type VARCHAR(40) NOT NULL COMMENT '文档类型',
  document_name VARCHAR(80) NOT NULL COMMENT '文档名称',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  is_current INT NOT NULL DEFAULT 1 COMMENT '是否当前版本',
  status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT '文档状态',
  locked INT NOT NULL DEFAULT 0 COMMENT '是否锁定',
  content_text LONGTEXT NULL COMMENT '文本内容',
  content_json LONGTEXT NULL COMMENT '结构化内容',
  completion_percent INT NOT NULL DEFAULT 0 COMMENT '完成度',
  required_valid INT NOT NULL DEFAULT 0 COMMENT '必填校验是否通过',
  submitted_at DATETIME NULL COMMENT '提交时间',
  submitted_by BIGINT NULL COMMENT '提交人',
  approved_at DATETIME NULL COMMENT '审批通过时间',
  approved_by BIGINT NULL COMMENT '审批人',
  rejected_at DATETIME NULL COMMENT '驳回时间',
  rejected_by BIGINT NULL COMMENT '驳回人',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  process_instance_id VARCHAR(100) NULL COMMENT '流程实例id',
  last_saved_at DATETIME NULL COMMENT '最后保存时间',
  remark VARCHAR(1000) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_certification_document_current (tenant_id, task_id, document_type, version_no, deleted),
  KEY idx_certification_document_task (tenant_id, task_id, document_type, is_current, deleted),
  KEY idx_certification_document_status (tenant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目文档';

CREATE TABLE IF NOT EXISTS certification_charter_goal (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT '章程文档id',
  goal_name VARCHAR(200) NOT NULL COMMENT '目标名称',
  goal_description VARCHAR(1000) NULL COMMENT '目标描述',
  target_date DATE NULL COMMENT '目标日期',
  completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_goal_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目章程目标';

CREATE TABLE IF NOT EXISTS certification_task_member (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT '成员文档id',
  member_name VARCHAR(80) NOT NULL COMMENT '成员姓名',
  role_code VARCHAR(60) NULL COMMENT '角色编码',
  role_name VARCHAR(80) NULL COMMENT '角色名称',
  department_name VARCHAR(120) NULL COMMENT '部门',
  responsibility VARCHAR(500) NULL COMMENT '职责',
  start_date DATE NULL COMMENT '投入开始日期',
  end_date DATE NULL COMMENT '投入结束日期',
  input_frequency VARCHAR(40) NULL COMMENT '投入频率',
  workload_percent DECIMAL(8,2) NULL COMMENT '投入比例',
  phone VARCHAR(30) NULL COMMENT '手机号',
  masked_phone VARCHAR(30) NULL COMMENT '脱敏手机号',
  core_role TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否核心角色',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_member_task (tenant_id, task_id, deleted),
  KEY idx_certification_member_role (tenant_id, role_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目成员';

CREATE TABLE IF NOT EXISTS certification_wbs_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT 'WBS文档id',
  parent_id BIGINT NULL COMMENT '父级WBS',
  wbs_code VARCHAR(60) NOT NULL COMMENT 'WBS编码',
  category_code VARCHAR(60) NULL COMMENT '分类编码',
  category_name VARCHAR(80) NULL COMMENT '分类名称',
  item_name VARCHAR(200) NOT NULL COMMENT '事项名称',
  activity_description VARCHAR(1000) NULL COMMENT '活动描述',
  estimate_hours DECIMAL(12,2) NULL COMMENT '估算工时',
  manpower_count DECIMAL(12,2) NULL COMMENT '人力数量',
  manpower_role_code VARCHAR(60) NULL COMMENT '人力角色',
  responsible_member_id BIGINT NULL COMMENT '负责成员',
  resource_input VARCHAR(500) NULL COMMENT '资源投入',
  cost_amount DECIMAL(14,2) NULL COMMENT '费用',
  duration_days INT NULL COMMENT '工期天数',
  predecessor_id BIGINT NULL COMMENT '前置事项',
  leaf TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否叶子节点',
  progress_percent INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
  status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '状态',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_certification_wbs_code (tenant_id, task_id, wbs_code, deleted),
  KEY idx_certification_wbs_task (tenant_id, task_id, deleted),
  KEY idx_certification_wbs_member (tenant_id, responsible_member_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目WBS';

CREATE TABLE IF NOT EXISTS certification_schedule_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT '进度文档id',
  day_no INT NULL COMMENT '第几天',
  plan_date DATE NULL COMMENT '计划日期',
  start_date DATE NULL COMMENT '开始日期',
  end_date DATE NULL COMMENT '结束日期',
  item_name VARCHAR(200) NOT NULL COMMENT '事项名称',
  item_description VARCHAR(1000) NULL COMMENT '事项说明',
  required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必做',
  completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
  external_contact_member_id BIGINT NULL COMMENT '外部联系人',
  certification_owner_member_id BIGINT NULL COMMENT '认定负责人',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_schedule_task (tenant_id, task_id, deleted),
  KEY idx_certification_schedule_date (tenant_id, start_date, end_date, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目进度';

CREATE TABLE IF NOT EXISTS certification_risk (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT '风险文档id',
  risk_no VARCHAR(60) NOT NULL COMMENT '风险编号',
  risk_description VARCHAR(1000) NOT NULL COMMENT '风险描述',
  probability_code VARCHAR(20) NOT NULL COMMENT '概率 low/medium/high',
  impact_code VARCHAR(20) NOT NULL COMMENT '影响 low/medium/high',
  risk_level_code VARCHAR(40) NOT NULL COMMENT '风险等级',
  risk_level_score INT NOT NULL COMMENT '风险分值',
  response_plan VARCHAR(1000) NULL COMMENT '应对计划',
  owner_member_id BIGINT NULL COMMENT '负责人项目成员',
  owner_user_id BIGINT NULL COMMENT '负责人用户',
  owner_name VARCHAR(80) NULL COMMENT '负责人姓名',
  status VARCHAR(40) NOT NULL DEFAULT 'open' COMMENT '状态',
  expected_close_date DATE NULL COMMENT '预计关闭日期',
  closed_at DATETIME NULL COMMENT '关闭时间',
  closed_by BIGINT NULL COMMENT '关闭人',
  close_reason VARCHAR(500) NULL COMMENT '关闭说明',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_risk_task (tenant_id, task_id, deleted),
  KEY idx_certification_risk_level (tenant_id, risk_level_score, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目风险';

CREATE TABLE IF NOT EXISTS certification_file_ref (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '任务id',
  document_id BIGINT NULL COMMENT '文档id',
  biz_type VARCHAR(40) NOT NULL COMMENT '业务类型',
  file_id BIGINT NOT NULL COMMENT '文件id',
  file_name VARCHAR(300) NULL COMMENT '文件名',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_file_task (tenant_id, task_id, biz_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定项目附件引用';

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6300, 1, 0, 'directory', 'certification', '认定管理', '/certification', NULL, 'certification.project', 'SafetyCertificateOutlined', 6, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (6301, 1, 6300, 'page', 'certification_project', '项目管理', '/certification/project', 'certification/ProjectManagement', 'certification.project.task.list', 'ProjectOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6900, 1, 6300, 'certification.project', '认定项目管理入口', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6901, 1, 6301, 'certification.project.task.select', '待接收任务订单', 'select', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6902, 1, 6301, 'certification.project.task.list', '查看认定项目任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6903, 1, 6301, 'certification.project.task.create', '新建认定项目任务', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6904, 1, 6301, 'certification.project.task.delete', '删除认定项目任务', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6905, 1, 6301, 'certification.project.task.flow', '查看任务流程', 'flow', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6906, 1, 6301, 'certification.project.charter.view', '查看任务章程', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6907, 1, 6301, 'certification.project.charter.edit', '编辑任务章程', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6908, 1, 6301, 'certification.project.charter.submit', '提交任务章程', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6909, 1, 6301, 'certification.project.members.view', '查看项目成员', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6910, 1, 6301, 'certification.project.members.edit', '编辑项目成员', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6911, 1, 6301, 'certification.project.members.submit', '提交项目成员', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6912, 1, 6301, 'certification.project.wbs.view', '查看项目WBS', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6913, 1, 6301, 'certification.project.wbs.edit', '编辑项目WBS', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6914, 1, 6301, 'certification.project.wbs.submit', '提交项目WBS', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6915, 1, 6301, 'certification.project.schedule.view', '查看项目进度', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6916, 1, 6301, 'certification.project.schedule.edit', '编辑项目进度', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6917, 1, 6301, 'certification.project.schedule.submit', '提交项目进度', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6918, 1, 6301, 'certification.project.risk.view', '查看风险管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6919, 1, 6301, 'certification.project.risk.edit', '编辑风险管理', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6920, 1, 6301, 'certification.project.risk.submit', '提交风险管理', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806300, 1, 2000, 6300, NOW(), NOW(), 1, 1, 0),
  (806301, 1, 2000, 6301, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (906900, 1, 2000, 6900, NOW(), NOW(), 1, 1, 0),
  (906901, 1, 2000, 6901, NOW(), NOW(), 1, 1, 0),
  (906902, 1, 2000, 6902, NOW(), NOW(), 1, 1, 0),
  (906903, 1, 2000, 6903, NOW(), NOW(), 1, 1, 0),
  (906904, 1, 2000, 6904, NOW(), NOW(), 1, 1, 0),
  (906905, 1, 2000, 6905, NOW(), NOW(), 1, 1, 0),
  (906906, 1, 2000, 6906, NOW(), NOW(), 1, 1, 0),
  (906907, 1, 2000, 6907, NOW(), NOW(), 1, 1, 0),
  (906908, 1, 2000, 6908, NOW(), NOW(), 1, 1, 0),
  (906909, 1, 2000, 6909, NOW(), NOW(), 1, 1, 0),
  (906910, 1, 2000, 6910, NOW(), NOW(), 1, 1, 0),
  (906911, 1, 2000, 6911, NOW(), NOW(), 1, 1, 0),
  (906912, 1, 2000, 6912, NOW(), NOW(), 1, 1, 0),
  (906913, 1, 2000, 6913, NOW(), NOW(), 1, 1, 0),
  (906914, 1, 2000, 6914, NOW(), NOW(), 1, 1, 0),
  (906915, 1, 2000, 6915, NOW(), NOW(), 1, 1, 0),
  (906916, 1, 2000, 6916, NOW(), NOW(), 1, 1, 0),
  (906917, 1, 2000, 6917, NOW(), NOW(), 1, 1, 0),
  (906918, 1, 2000, 6918, NOW(), NOW(), 1, 1, 0),
  (906919, 1, 2000, 6919, NOW(), NOW(), 1, 1, 0),
  (906920, 1, 2000, 6920, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_task (
  id, tenant_id, task_no, task_name, task_manager_name, company_name, certification_type, work_type_name,
  level_code, level_name, planned_people_count, plan_start_date, plan_end_date, exam_date, source_type,
  source_name, current_phase, status, document_completed_count, document_total_count, phase_completed_count,
  phase_total_count, last_business_updated_at, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202605290001, 1, 'RD-2026-001', '2026年职业技能认定任务', '张晓峰', '千铝国昱', 'with_question_bank', '电工', 'senior', '高级', 60, '2026-05-08', '2026-05-20', '2026-05-20', 'manual', '手动新增', 'preparation', 'running', 1, 5, 1, 5, NOW(), '示例任务', NOW(), NOW(), 1, 1, 0),
  (202605290002, 1, 'RD-2026-002', '企业专项认定协同任务', '周楠', '国昱制造', 'enterprise_special', '焊工', 'intermediate', '中级', 42, '2026-05-12', '2026-05-28', '2026-05-28', 'order', '任务订单', 'before_exam', 'running', 2, 5, 1, 5, NOW(), '示例任务', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_task_phase (
  id, tenant_id, task_id, phase_code, phase_name, phase_order, status, started_at, completed_at, completed_by,
  remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202605291001, 1, 202605290001, 'preparation', '任务准备阶段', 1, 'running', NOW(), NULL, NULL, NULL, NOW(), NOW(), 1, 1, 0),
  (202605291002, 1, 202605290001, 'before_exam', '考前准备阶段', 2, 'pending', NULL, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 0),
  (202605291003, 1, 202605290001, 'theory_exam', '理论考试阶段', 3, 'pending', NULL, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 0),
  (202605291004, 1, 202605290001, 'practical_exam', '实操考试阶段', 4, 'pending', NULL, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 0),
  (202605291005, 1, 202605290001, 'archive', '归档总结阶段', 5, 'pending', NULL, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_task_document (
  id, tenant_id, task_id, document_type, document_name, version_no, is_current, status, locked, content_text,
  content_json, completion_percent, required_valid, submitted_at, submitted_by, approved_at, approved_by,
  rejected_at, rejected_by, reject_reason, process_instance_id, last_saved_at, remark, created_at, updated_at,
  created_by, updated_by, deleted
) VALUES
  (202605292001, 1, 202605290001, 'charter', '任务章程', 1, 1, 'draft', 0, '明确认定任务范围、目标、交付物与关键日期。', NULL, 40, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NULL, NOW(), NOW(), 1, 1, 0),
  (202605292002, 1, 202605290001, 'members', '项目成员', 1, 1, 'draft', 0, NULL, NULL, 30, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NULL, NOW(), NOW(), 1, 1, 0),
  (202605292003, 1, 202605290001, 'wbs', '项目WBS', 1, 1, 'draft', 0, NULL, NULL, 20, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NULL, NOW(), NOW(), 1, 1, 0),
  (202605292004, 1, 202605290001, 'schedule', '项目进度', 1, 1, 'draft', 0, NULL, NULL, 20, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NULL, NOW(), NOW(), 1, 1, 0),
  (202605292005, 1, 202605290001, 'risk', '风险管理', 1, 1, 'draft', 0, NULL, NULL, 20, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NULL, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_task_member (
  id, tenant_id, task_id, document_id, member_name, role_code, role_name, department_name, responsibility,
  start_date, end_date, input_frequency, workload_percent, phone, masked_phone, core_role, sort_order, remark,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202605293001, 1, 202605290001, 202605292002, '李敏', 'material_specialist', '资料专员', '运营部', '整理申报资料', '2026-05-08', '2026-05-20', 'full', 80.00, '13800001234', '138****1234', 1, 1, NULL, NOW(), NOW(), 1, 1, 0),
  (202605293002, 1, 202605290001, 202605292002, '王强', 'certification_owner', '认定负责人', '鉴定中心', '协调认定流程', '2026-05-08', '2026-05-20', 'full', 60.00, '13900001234', '139****1234', 1, 2, NULL, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_wbs_item (
  id, tenant_id, task_id, document_id, parent_id, wbs_code, category_code, category_name, item_name,
  activity_description, estimate_hours, manpower_count, manpower_role_code, responsible_member_id, resource_input,
  cost_amount, duration_days, predecessor_id, leaf, progress_percent, status, sort_order, remark, created_at,
  updated_at, created_by, updated_by, deleted
) VALUES
  (202605294001, 1, 202605290001, 202605292003, NULL, '1', 'planning', '规划', '任务立项与策划', '确认范围、计划和资源', 8.00, 1.00, 'task_manager', 202605293001, '会议室', 0.00, 1, NULL, 1, 30, 'running', 1, NULL, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_schedule_item (
  id, tenant_id, task_id, document_id, day_no, plan_date, start_date, end_date, item_name, item_description,
  required, completed, external_contact_member_id, certification_owner_member_id, sort_order, remark, created_at,
  updated_at, created_by, updated_by, deleted
) VALUES
  (202605295001, 1, 202605290001, 202605292004, 1, '2026-05-08', '2026-05-08', '2026-05-08', '确认考试时间', '与企业确认理论与实操考试时间', 1, 0, 202605293001, 202605293002, 1, NULL, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO certification_risk (
  id, tenant_id, task_id, document_id, risk_no, risk_description, probability_code, impact_code, risk_level_code,
  risk_level_score, response_plan, owner_member_id, owner_user_id, owner_name, status, expected_close_date,
  closed_at, closed_by, close_reason, sort_order, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202605296001, 1, 202605290001, 202605292005, 'R-001', '资料未按期补齐', 'high', 'medium', 'medium_high', 4, '提前催办并设置备用材料清单', 202605293001, NULL, '张晓峰', 'open', '2026-05-18', NULL, NULL, NULL, 1, NULL, NOW(), NOW(), 1, 1, 0);
