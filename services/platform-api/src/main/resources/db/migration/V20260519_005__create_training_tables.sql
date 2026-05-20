-- ============================================================
-- 培训管理模块全部数据库表
-- 包含：培训需求/方案/请求/申报/人员/班级/课程/资源/任务/考试/预算/证书
-- 以及相关的支撑表
-- ============================================================

-- ==================== 1. 培训需求 (train_need) ====================
CREATE TABLE IF NOT EXISTS train_need (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT NULL COMMENT '组织ID',
  org_name VARCHAR(255) NULL COMMENT '组织名称',
  train_object VARCHAR(255) NULL COMMENT '培训对象',
  position VARCHAR(255) NULL COMMENT '岗位',
  num INT NULL COMMENT '人数',
  work_type VARCHAR(255) NULL COMMENT '工种',
  level VARCHAR(255) NULL COMMENT '等级',
  target VARCHAR(500) NULL COMMENT '培训目标',
  method VARCHAR(255) NULL COMMENT '培训方式',
  address VARCHAR(255) NULL COMMENT '培训地点',
  train_date VARCHAR(255) NULL COMMENT '培训日期',
  budget VARCHAR(255) NULL COMMENT '预算',
  teacher_require VARCHAR(500) NULL COMMENT '师资要求',
  need_exam INT NULL DEFAULT 0 COMMENT '是否需要考试 0-否 1-是',
  need_match INT NULL DEFAULT 0 COMMENT '是否需要匹配 0-否 1-是',
  need_paper INT NULL DEFAULT 0 COMMENT '是否需要试卷 0-否 1-是',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  json_data TEXT NULL COMMENT '扩展JSON数据',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  INDEX idx_train_need_tenant (tenant_id),
  INDEX idx_train_need_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 2. 培训方案 (train_solution) ====================
CREATE TABLE IF NOT EXISTS train_solution (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_info_id BIGINT NULL COMMENT '培训信息ID',
  company_id BIGINT NULL COMMENT '企业ID',
  company_name VARCHAR(255) NULL COMMENT '企业名称',
  title VARCHAR(255) NULL COMMENT '标题',
  nation_policy TEXT NULL COMMENT '国家政策',
  industry_situation_analysis TEXT NULL COMMENT '行业形势分析',
  company_development_analysis TEXT NULL COMMENT '企业发展分析',
  research_data_analysis TEXT NULL COMMENT '调研数据分析',
  project_development_ideas TEXT NULL COMMENT '项目开发思路',
  main_objectives TEXT NULL COMMENT '主要目标',
  main_initiatives TEXT NULL COMMENT '主要举措',
  development_plan TEXT NULL COMMENT '发展计划',
  division TEXT NULL COMMENT '分工',
  risk TEXT NULL COMMENT '风险',
  communication TEXT NULL COMMENT '沟通',
  checkpoint TEXT NULL COMMENT '检查点',
  results TEXT NULL COMMENT '成果',
  teacher_intro TEXT NULL COMMENT '师资介绍',
  request_analysis TEXT NULL COMMENT '需求分析',
  train_content TEXT NULL COMMENT '培训内容',
  train_method TEXT NULL COMMENT '培训方式',
  course TEXT NULL COMMENT '课程',
  status INT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-待审批 2-已通过 3-已驳回',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  user_id BIGINT NULL COMMENT '用户ID',
  content TEXT NULL COMMENT '内容',
  file_path VARCHAR(500) NULL COMMENT '文件路径',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_solution_tenant (tenant_id),
  INDEX idx_train_solution_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 3. 培训请求 (train_request) ====================
CREATE TABLE IF NOT EXISTS train_request (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(255) NULL COMMENT '企业名称',
  company_id BIGINT NULL COMMENT '企业ID',
  position VARCHAR(255) NULL COMMENT '岗位',
  num INT NULL COMMENT '人数',
  type VARCHAR(100) NULL COMMENT '类型',
  train_date VARCHAR(255) NULL COMMENT '培训日期',
  contact VARCHAR(255) NULL COMMENT '联系人',
  contact_phone VARCHAR(50) NULL COMMENT '联系电话',
  is_on_position INT NULL DEFAULT 0 COMMENT '是否在岗 0-否 1-是',
  student_form VARCHAR(255) NULL COMMENT '学员形式',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_request_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 4. 培训申报 (train_declare) ====================
CREATE TABLE IF NOT EXISTS train_declare (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  title VARCHAR(255) NULL COMMENT '标题',
  city_id BIGINT NULL COMMENT '城市ID',
  city_name VARCHAR(255) NULL COMMENT '城市名称',
  user_id BIGINT NULL COMMENT '用户ID',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_declare_tenant (tenant_id),
  INDEX idx_train_declare_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 5. 培训人员 (train_people_list) ====================
CREATE TABLE IF NOT EXISTS train_people_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  train_class_id BIGINT NULL COMMENT '班级ID',
  type VARCHAR(50) NULL COMMENT '类型',
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  realname VARCHAR(255) NULL COMMENT '真实姓名',
  phone VARCHAR(50) NULL COMMENT '电话',
  is_leader INT NULL DEFAULT 0 COMMENT '是否组长 0-否 1-是',
  is_committee INT NULL DEFAULT 0 COMMENT '是否班委 0-否 1-是',
  exam_score DECIMAL(10,2) NULL COMMENT '考试成绩',
  real_score DECIMAL(10,2) NULL COMMENT '实操成绩',
  paper VARCHAR(255) NULL COMMENT '证书',
  normal_score DECIMAL(10,2) NULL COMMENT '平时成绩',
  national_paper VARCHAR(255) NULL COMMENT '国家证书',
  current_level INT NULL COMMENT '当前等级',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  id_card VARCHAR(50) NULL COMMENT '身份证号',
  status INT NULL DEFAULT 0 COMMENT '状态',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_people_tenant (tenant_id),
  INDEX idx_train_people_train (train_id),
  INDEX idx_train_people_class (train_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 6. 班级管理 (train_class_list) ====================
CREATE TABLE IF NOT EXISTS train_class_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  user_id BIGINT NULL COMMENT '用户ID',
  is_leader INT NULL DEFAULT 0 COMMENT '是否组长 0-否 1-是',
  is_committee INT NULL DEFAULT 0 COMMENT '是否班委 0-否 1-是',
  class_name VARCHAR(255) NULL COMMENT '班级名称',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  status INT NULL DEFAULT 0 COMMENT '状态 0-未开班 1-开班 2-结课',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_class_tenant (tenant_id),
  INDEX idx_train_class_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 7. 课程管理 (train_course) ====================
CREATE TABLE IF NOT EXISTS train_course (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  teacher_id_card VARCHAR(50) NULL COMMENT '教师身份证',
  teacher_name VARCHAR(255) NULL COMMENT '教师姓名',
  teacher_user_id BIGINT NULL COMMENT '教师用户ID',
  type INT NULL COMMENT '类型',
  course_date VARCHAR(255) NULL COMMENT '课程日期',
  title VARCHAR(255) NULL COMMENT '课程标题',
  start_time VARCHAR(50) NULL COMMENT '开始时间',
  end_time VARCHAR(50) NULL COMMENT '结束时间',
  theory_time INT NULL COMMENT '理论学时',
  operation_time INT NULL COMMENT '实操学时',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_course_tenant (tenant_id),
  INDEX idx_train_course_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 8. 课件管理 (course_kejian) ====================
CREATE TABLE IF NOT EXISTS course_kejian (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sno VARCHAR(100) NULL COMMENT '编号',
  title VARCHAR(255) NULL COMMENT '标题',
  keshi INT NULL COMMENT '课时',
  cover VARCHAR(500) NULL COMMENT '封面',
  type INT NULL COMMENT '类型',
  price DECIMAL(10,2) NULL COMMENT '价格',
  desction TEXT NULL COMMENT '描述',
  content TEXT NULL COMMENT '内容',
  file_path VARCHAR(500) NULL COMMENT '文件路径',
  work_type_id BIGINT NULL COMMENT '工种ID',
  tag INT NULL COMMENT '标签',
  jigou_id BIGINT NULL COMMENT '机构ID',
  owner_id BIGINT NULL COMMENT '所有者ID',
  addtime BIGINT NULL COMMENT '添加时间(时间戳)',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_course_kejian_work_type (work_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 9. 资源管理 (train_resource) ====================
CREATE TABLE IF NOT EXISTS train_resource (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_type_id BIGINT NULL COMMENT '工种ID',
  title VARCHAR(255) NULL COMMENT '标题',
  type VARCHAR(100) NULL COMMENT '类型',
  path VARCHAR(500) NULL COMMENT '路径',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_resource_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 10. 任务管理 (train_task_list) ====================
CREATE TABLE IF NOT EXISTS train_task_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_type_id BIGINT NULL COMMENT '工种ID',
  course_id BIGINT NULL COMMENT '课程ID',
  knowledge_id BIGINT NULL COMMENT '知识ID',
  title VARCHAR(255) NOT NULL COMMENT '标题',
  type VARCHAR(100) NULL COMMENT '类型',
  content TEXT NULL COMMENT '内容',
  get_learn_time DECIMAL(10,2) NULL COMMENT '获取学习时间',
  min_minute INT NULL COMMENT '最少分钟',
  is_required INT NULL DEFAULT 0 COMMENT '是否必修 0-否 1-是',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  score INT NULL COMMENT '分数',
  get_point DECIMAL(10,2) NULL COMMENT '获取积分',
  check_status INT NULL COMMENT '审核状态',
  final_check_status INT NULL COMMENT '终审状态',
  check_time DATETIME NULL COMMENT '审核时间',
  check_user_id BIGINT NULL COMMENT '审核人ID',
  check_username VARCHAR(255) NULL COMMENT '审核人姓名',
  final_check_time DATETIME NULL COMMENT '终审时间',
  final_check_user_id BIGINT NULL COMMENT '终审人ID',
  final_check_username VARCHAR(255) NULL COMMENT '终审人姓名',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_task_tenant (tenant_id),
  INDEX idx_train_task_work_type (work_type_id),
  INDEX idx_train_task_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 11. 培训配置任务 (train_task_info) ====================
CREATE TABLE IF NOT EXISTS train_task_info (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  cover VARCHAR(500) NULL COMMENT '封面',
  course_id BIGINT NULL COMMENT '课程ID',
  train_task_id BIGINT NULL COMMENT '培训任务ID',
  knowledge_id BIGINT NULL COMMENT '知识ID',
  intro TEXT NULL COMMENT '简介',
  is_required INT NULL DEFAULT 0 COMMENT '是否必修',
  score INT NULL COMMENT '分数',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_task_info_tenant (tenant_id),
  INDEX idx_train_task_info_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 12. 考试管理 (train_exam_list) ====================
CREATE TABLE IF NOT EXISTS train_exam_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NULL COMMENT '标题',
  level VARCHAR(100) NULL COMMENT '等级',
  train_id BIGINT NULL COMMENT '培训ID',
  org_id BIGINT NULL COMMENT '组织ID',
  org_name VARCHAR(255) NULL COMMENT '组织名称',
  type INT NULL COMMENT '类型',
  source INT NULL COMMENT '来源',
  invigilate_user_id BIGINT NULL COMMENT '监考人ID',
  invigilate_username VARCHAR(255) NULL COMMENT '监考人姓名',
  evaluate_user_id BIGINT NULL COMMENT '评价人ID',
  evaluate_username VARCHAR(255) NULL COMMENT '评价人姓名',
  watcher_user_id BIGINT NULL COMMENT '观察员ID',
  watcher_username VARCHAR(255) NULL COMMENT '观察员姓名',
  staff_user_id BIGINT NULL COMMENT '工作人员ID',
  staff_username VARCHAR(255) NULL COMMENT '工作人员姓名',
  proposition_user_id BIGINT NULL COMMENT '命题人ID',
  proposition_username VARCHAR(255) NULL COMMENT '命题人姓名',
  marking_user_id BIGINT NULL COMMENT '阅卷人ID',
  marking_username VARCHAR(255) NULL COMMENT '阅卷人姓名',
  exam_id BIGINT NULL COMMENT '考试ID',
  exam_date VARCHAR(255) NULL COMMENT '考试日期',
  exam_date_mode INT NULL COMMENT '考试日期模式',
  exam_date_list TEXT NULL COMMENT '考试日期列表',
  exam_time INT NULL COMMENT '考试时长(分钟)',
  need_finish_adventure INT NULL DEFAULT 0 COMMENT '需完成冒险',
  need_finish_normal INT NULL DEFAULT 0 COMMENT '需完成普通',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  jigou_id BIGINT NULL COMMENT '机构ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_exam_tenant (tenant_id),
  INDEX idx_train_exam_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 13. 考试结果 (exam_result) ====================
CREATE TABLE IF NOT EXISTS exam_result (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(50) NOT NULL COMMENT '类型',
  exam_plan_id BIGINT NULL COMMENT '考试计划ID',
  paper_id BIGINT NULL COMMENT '试卷ID',
  detail_id BIGINT NULL COMMENT '详情ID',
  get_score DECIMAL(10,2) NULL COMMENT '得分',
  exam_type INT NULL COMMENT '考试类型',
  exam_id BIGINT NULL COMMENT '考试ID',
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  user_answer TEXT NOT NULL COMMENT '用户答案',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_result_tenant (tenant_id),
  INDEX idx_exam_result_exam (exam_id),
  INDEX idx_exam_result_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 14. 预算管理 (train_budget) ====================
CREATE TABLE IF NOT EXISTS train_budget (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  province VARCHAR(100) NULL COMMENT '省',
  city VARCHAR(100) NULL COMMENT '市',
  region VARCHAR(100) NULL COMMENT '区',
  org_id BIGINT NULL COMMENT '企业ID',
  org_name VARCHAR(255) NULL COMMENT '企业名称',
  train_date VARCHAR(255) NULL COMMENT '培训时间',
  train_work_type_id BIGINT NULL COMMENT '培训工种ID',
  train_work_type_name VARCHAR(255) NULL COMMENT '培训工种',
  train_level VARCHAR(100) NULL COMMENT '培训级别',
  bonus_standard VARCHAR(255) NULL COMMENT '补贴标准',
  expect_income DECIMAL(16,2) NULL COMMENT '预计收入',
  actual_income DECIMAL(16,2) NULL COMMENT '实际收入',
  school_manage_fee DECIMAL(16,2) NULL COMMENT '学校管理费',
  manage_fee DECIMAL(16,2) NULL COMMENT '公司运营管理费',
  research_fee DECIMAL(16,2) NULL COMMENT '研发费用',
  company_require_commission DECIMAL(16,2) NULL COMMENT '企业要求分成',
  agent_require_commission DECIMAL(16,2) NULL COMMENT '中介要求分成',
  teacher_course_fee DECIMAL(16,2) NULL COMMENT '培训讲师课酬',
  teacher_fee DECIMAL(16,2) NULL COMMENT '培训讲师食宿差旅费',
  staff_fee DECIMAL(16,2) NULL COMMENT '工作人员劳务费用',
  staff_travel_fee DECIMAL(16,2) NULL COMMENT '工作人员食宿差旅费',
  train_fee DECIMAL(16,2) NULL COMMENT '培训耗材费',
  point_rewards DECIMAL(16,2) NULL COMMENT '积分奖励',
  business_fee DECIMAL(16,2) NULL COMMENT '业务招待费',
  people_num INT NULL COMMENT '认定人数',
  charge_fee VARCHAR(255) NULL COMMENT '收费标准',
  service_fee DECIMAL(16,2) NULL COMMENT '行业服务费',
  review_fee DECIMAL(16,2) NULL COMMENT '评审费用',
  maintain_fee DECIMAL(16,2) NULL COMMENT '认定金额',
  makeup_people_num INT NULL COMMENT '补考人数',
  theory_makeup_people_num INT NULL COMMENT '理论补考',
  operation_makeup_people_num INT NULL COMMENT '实操补考',
  staff_makeup_train_fee DECIMAL(16,2) NULL COMMENT '工作人员差旅费',
  review_makeup_train_fee DECIMAL(16,2) NULL COMMENT '考评员差旅费',
  profit DECIMAL(16,2) NULL COMMENT '项目利润',
  comments TEXT NULL COMMENT '备注',
  status INT NULL DEFAULT 0 COMMENT '状态 0-正常 1-冻结',
  user_id BIGINT NULL COMMENT '用户ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_budget_tenant (tenant_id),
  INDEX idx_train_budget_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 15. 证书管理 (train_certificate) ====================
CREATE TABLE IF NOT EXISTS train_certificate (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  user_id BIGINT NULL COMMENT '用户ID',
  user_name VARCHAR(255) NULL COMMENT '用户名',
  certificate_no VARCHAR(255) NULL COMMENT '证书编号',
  certificate_type VARCHAR(100) NULL COMMENT '证书类型',
  work_type VARCHAR(255) NULL COMMENT '工种',
  level VARCHAR(100) NULL COMMENT '等级',
  issue_date VARCHAR(50) NULL COMMENT '发证日期',
  expire_date VARCHAR(50) NULL COMMENT '过期日期',
  issuing_authority VARCHAR(255) NULL COMMENT '发证机构',
  file_path VARCHAR(500) NULL COMMENT '文件路径',
  status INT NULL DEFAULT 1 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  created_by BIGINT NULL COMMENT '创建人',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_certificate_tenant (tenant_id),
  INDEX idx_train_certificate_project (project_id),
  INDEX idx_train_certificate_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 16. 人才数据管理 (train_talent_needs) ====================
CREATE TABLE IF NOT EXISTS train_talent_needs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NULL COMMENT '标题',
  org_id BIGINT NULL COMMENT '组织ID',
  org_name VARCHAR(255) NULL COMMENT '组织名称',
  job VARCHAR(255) NULL COMMENT '岗位',
  work_type VARCHAR(255) NULL COMMENT '工种',
  major VARCHAR(255) NULL COMMENT '专业',
  edu VARCHAR(100) NULL COMMENT '学历',
  gender INT NULL COMMENT '性别 0-不限 1-男 2-女',
  num INT NULL COMMENT '人数',
  address VARCHAR(255) NULL COMMENT '地址',
  work_year VARCHAR(50) NULL COMMENT '工作年限',
  in_position_date VARCHAR(50) NULL COMMENT '到岗日期',
  paper_require VARCHAR(255) NULL COMMENT '证书要求',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  json_data TEXT NULL COMMENT '扩展JSON数据',
  duty TEXT NULL COMMENT '职责',
  school_need TEXT NULL COMMENT '学校需求',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_talent_needs_tenant (tenant_id),
  INDEX idx_train_talent_needs_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 17. 师资管理 (train_teacher_bank) ====================
CREATE TABLE IF NOT EXISTS train_teacher_bank (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NULL COMMENT '姓名',
  phone VARCHAR(50) NULL COMMENT '电话',
  id_card VARCHAR(50) NULL COMMENT '身份证',
  teacher_license VARCHAR(255) NULL COMMENT '教师资格证',
  engineer_license VARCHAR(255) NULL COMMENT '工程师证',
  level_license VARCHAR(255) NULL COMMENT '等级证',
  edu_license VARCHAR(255) NULL COMMENT '学历证',
  graduate_paper VARCHAR(255) NULL COMMENT '毕业证',
  company VARCHAR(255) NULL COMMENT '公司',
  address VARCHAR(500) NULL COMMENT '地址',
  id_card_number VARCHAR(50) NULL COMMENT '身份证号',
  start_date VARCHAR(50) NULL COMMENT '开始日期',
  end_date VARCHAR(50) NULL COMMENT '结束日期',
  birthday VARCHAR(50) NULL COMMENT '生日',
  gender INT NULL COMMENT '性别',
  nation VARCHAR(50) NULL COMMENT '民族',
  education VARCHAR(100) NULL COMMENT '学历',
  major VARCHAR(255) NULL COMMENT '专业',
  status INT NULL DEFAULT 1 COMMENT '状态',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_teacher_bank_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 18. 文件管理主表 (train_file_list) ====================
CREATE TABLE IF NOT EXISTS train_file_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(255) NULL COMMENT '关联表名',
  pk VARCHAR(100) NULL COMMENT '关联主键',
  title VARCHAR(255) NULL COMMENT '标题',
  user_id BIGINT NULL COMMENT '用户ID',
  status INT NULL DEFAULT 1 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_file_list_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 19. 文件明细 (train_file_detail) ====================
CREATE TABLE IF NOT EXISTS train_file_detail (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  file_id BIGINT NULL COMMENT '文件主表ID',
  title VARCHAR(255) NULL COMMENT '标题',
  path VARCHAR(500) NULL COMMENT '路径',
  user_id BIGINT NULL COMMENT '用户ID',
  status INT NULL DEFAULT 1 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_file_detail_tenant (tenant_id),
  INDEX idx_train_file_detail_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 20. 培训信息主表 (train_info_list) ====================
CREATE TABLE IF NOT EXISTS train_info_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NULL COMMENT '标题',
  cover VARCHAR(500) NULL COMMENT '封面',
  intro TEXT NULL COMMENT '简介',
  group_type INT NULL COMMENT '组类型',
  master_type INT NULL COMMENT '主类型',
  type INT NULL COMMENT '类型',
  cate VARCHAR(100) NULL COMMENT '分类',
  company_id BIGINT NULL COMMENT '企业ID',
  company_name VARCHAR(255) NULL COMMENT '企业名称',
  company_address VARCHAR(500) NULL COMMENT '企业地址',
  work_type_id BIGINT NULL COMMENT '工种ID',
  work_type_level INT NULL COMMENT '工种等级',
  course_id BIGINT NULL COMMENT '课程ID',
  num INT NULL COMMENT '人数',
  position VARCHAR(255) NULL COMMENT '岗位',
  course_num INT NULL COMMENT '课程数',
  head_confirm INT NULL COMMENT '负责人确认',
  user_id BIGINT NULL COMMENT '用户ID',
  company_has_confirm INT NULL COMMENT '企业已确认',
  company_confirm_time DATETIME NULL COMMENT '企业确认时间',
  company_confirm_user_id BIGINT NULL COMMENT '企业确认人ID',
  company_confirm_username VARCHAR(255) NULL COMMENT '企业确认人姓名',
  pass_status INT NULL COMMENT '通过状态',
  check_user_id BIGINT NULL COMMENT '审核人ID',
  check_username VARCHAR(255) NULL COMMENT '审核人姓名',
  is_sorted_learn INT NULL COMMENT '是否排序学习',
  max_normal_score DECIMAL(10,2) NULL COMMENT '最高平时成绩',
  course_file VARCHAR(500) NULL COMMENT '课程文件',
  start_date VARCHAR(50) NULL COMMENT '开始日期',
  end_date VARCHAR(50) NULL COMMENT '结束日期',
  train_company_name VARCHAR(255) NULL COMMENT '培训企业名称',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  point INT NULL COMMENT '积分',
  album VARCHAR(500) NULL COMMENT '相册',
  summary TEXT NULL COMMENT '总结',
  province_id BIGINT NULL COMMENT '省ID',
  province_name VARCHAR(255) NULL COMMENT '省名称',
  city_id BIGINT NULL COMMENT '市ID',
  city_name VARCHAR(255) NULL COMMENT '市名称',
  ability_score DECIMAL(10,2) NULL COMMENT '能力分',
  region_id BIGINT NULL COMMENT '区ID',
  region_name VARCHAR(255) NULL COMMENT '区名称',
  in_charge_user_id BIGINT NULL COMMENT '负责人ID',
  in_charge_username VARCHAR(255) NULL COMMENT '负责人姓名',
  files TEXT NULL COMMENT '文件列表JSON',
  traindemand_id BIGINT NULL COMMENT '培训需求ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_info_list_tenant (tenant_id),
  INDEX idx_train_info_list_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 21. 培训班级信息 (train_info_class) ====================
CREATE TABLE IF NOT EXISTS train_info_class (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_info_id BIGINT NULL COMMENT '培训信息ID',
  title VARCHAR(255) NULL COMMENT '标题',
  nums INT NULL COMMENT '人数',
  company_id BIGINT NULL COMMENT '企业ID',
  work_type_id BIGINT NULL COMMENT '工种ID',
  date_start VARCHAR(50) NULL COMMENT '开始日期',
  date_end VARCHAR(50) NULL COMMENT '结束日期',
  addtime INT NULL COMMENT '添加时间(时间戳)',
  file_temps TEXT NULL COMMENT '文件模板JSON',
  kcbiao_file VARCHAR(500) NULL COMMENT '课程表文件',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_info_class_train (train_info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 22. 任务培训配置详情 (train_task_info_detail) ====================
CREATE TABLE IF NOT EXISTS train_task_info_detail (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  task_list_id BIGINT NULL COMMENT '任务ID',
  work_type_id BIGINT NULL COMMENT '工种ID',
  course_id BIGINT NULL COMMENT '课程ID',
  knowledge_id BIGINT NULL COMMENT '知识ID',
  title VARCHAR(255) NULL COMMENT '标题',
  type VARCHAR(100) NULL COMMENT '类型',
  content TEXT NULL COMMENT '内容',
  get_learn_time DECIMAL(10,2) NULL COMMENT '获取学习时间',
  min_minute INT NULL COMMENT '最少分钟',
  is_required INT NULL DEFAULT 0 COMMENT '是否必修',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  score INT NULL COMMENT '分数',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_task_info_detail_tenant (tenant_id),
  INDEX idx_train_task_info_detail_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 23. 任务日志 (train_task_log) ====================
CREATE TABLE IF NOT EXISTS train_task_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_class_id BIGINT NULL COMMENT '班级ID',
  ck_id BIGINT NULL COMMENT '签到ID',
  date VARCHAR(50) NULL COMMENT '日期',
  addtime INT NULL COMMENT '添加时间(时间戳)',
  status INT NULL COMMENT '状态',
  times VARCHAR(50) NULL COMMENT '时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_task_log_class (train_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 24. 用户学习记录 (train_user_learn_log) ====================
CREATE TABLE IF NOT EXISTS train_user_learn_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  match_id BIGINT NULL COMMENT '匹配ID',
  train_task_id BIGINT NULL COMMENT '培训任务ID',
  learn_time INT NULL COMMENT '学习时间(分钟)',
  score INT NULL COMMENT '分数',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_learn_log_tenant (tenant_id),
  INDEX idx_train_user_learn_log_user (user_id),
  INDEX idx_train_user_learn_log_train (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 25. 用户选课计划 (train_user_course_plan) ====================
CREATE TABLE IF NOT EXISTS train_user_course_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  realname VARCHAR(255) NULL COMMENT '真实姓名',
  add_from INT NULL COMMENT '添加来源',
  course_id BIGINT NULL COMMENT '课程ID',
  date VARCHAR(50) NULL COMMENT '日期',
  type INT NULL COMMENT '类型',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_course_plan_tenant (tenant_id),
  INDEX idx_train_user_course_plan_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 26. 用户课程证书 (train_user_course_paper) ====================
CREATE TABLE IF NOT EXISTS train_user_course_paper (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  course_id BIGINT NULL COMMENT '课程ID',
  work_type_id BIGINT NULL COMMENT '工种ID',
  get_year VARCHAR(20) NULL COMMENT '获取年份',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_course_paper_tenant (tenant_id),
  INDEX idx_train_user_course_paper_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 27. 用户证书 (train_user_paper) ====================
CREATE TABLE IF NOT EXISTS train_user_paper (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  url VARCHAR(500) NULL COMMENT 'URL',
  title VARCHAR(255) NULL COMMENT '标题',
  type VARCHAR(100) NULL COMMENT '类型',
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  pk BIGINT NULL COMMENT '关联主键',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_paper_tenant (tenant_id),
  INDEX idx_train_user_paper_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 28. 用户专利 (train_user_patent) ====================
CREATE TABLE IF NOT EXISTS train_user_patent (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NULL COMMENT '标题',
  paper VARCHAR(255) NULL COMMENT '证书',
  user_id BIGINT NULL COMMENT '用户ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_patent_tenant (tenant_id),
  INDEX idx_train_user_patent_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 29. 用户考试日志 (train_user_exam_log) ====================
CREATE TABLE IF NOT EXISTS train_user_exam_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  knowledge_id BIGINT NULL COMMENT '知识ID',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_exam_log_tenant (tenant_id),
  INDEX idx_train_user_exam_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 30. 用户能力 (train_user_ability) ====================
CREATE TABLE IF NOT EXISTS train_user_ability (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_id BIGINT NULL COMMENT '培训ID',
  course_id BIGINT NULL COMMENT '课程ID',
  knowledge_id BIGINT NULL COMMENT '知识ID',
  result VARCHAR(500) NULL COMMENT '结果',
  config_id BIGINT NULL COMMENT '配置ID',
  score INT NULL COMMENT '分数',
  current_score INT NULL COMMENT '当前分数',
  user_id BIGINT NULL COMMENT '用户ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  comments TEXT NULL COMMENT '备注',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_ability_tenant (tenant_id),
  INDEX idx_train_user_ability_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 31. 项目管理 (train_project) ====================
CREATE TABLE IF NOT EXISTS train_project (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_name VARCHAR(255) NULL COMMENT '项目名称',
  company_id BIGINT NULL COMMENT '企业ID',
  company_name VARCHAR(255) NULL COMMENT '企业名称',
  manager_id BIGINT NULL COMMENT '负责人ID',
  manager_name VARCHAR(255) NULL COMMENT '负责人姓名',
  plan_start VARCHAR(50) NULL COMMENT '计划开始',
  plan_end VARCHAR(50) NULL COMMENT '计划结束',
  current_stage VARCHAR(100) NULL COMMENT '当前阶段',
  progress INT NULL DEFAULT 0 COMMENT '进度',
  status INT NULL DEFAULT 0 COMMENT '状态',
  description TEXT NULL COMMENT '描述',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  created_at DATETIME NULL COMMENT '创建时间',
  updated_at DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_project_tenant (tenant_id),
  INDEX idx_train_project_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 32. 十大管理表 (train_project_table) ====================
CREATE TABLE IF NOT EXISTS train_project_table (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  table_type INT NULL COMMENT '表类型',
  table_name VARCHAR(255) NULL COMMENT '表名称',
  content TEXT NULL COMMENT '内容',
  version INT NULL DEFAULT 1 COMMENT '版本',
  status INT NULL DEFAULT 0 COMMENT '状态',
  file_id BIGINT NULL COMMENT '文件ID',
  owner_id BIGINT NULL COMMENT '负责人ID',
  owner_name VARCHAR(255) NULL COMMENT '负责人姓名',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_project_table_tenant (tenant_id),
  INDEX idx_train_project_table_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 33. 协议管理 (train_protocol) ====================
CREATE TABLE IF NOT EXISTS train_protocol (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  title VARCHAR(255) NULL COMMENT '标题',
  type VARCHAR(100) NULL COMMENT '类型',
  content TEXT NULL COMMENT '内容',
  version INT NULL DEFAULT 1 COMMENT '版本',
  status INT NULL DEFAULT 0 COMMENT '状态',
  signed_at VARCHAR(50) NULL COMMENT '签署时间',
  signer_id BIGINT NULL COMMENT '签署人ID',
  approver_id BIGINT NULL COMMENT '审批人ID',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_protocol_tenant (tenant_id),
  INDEX idx_train_protocol_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 34. 用户信息 (train_user_info) ====================
CREATE TABLE IF NOT EXISTS train_user_info (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL COMMENT '用户ID',
  company_id BIGINT NULL COMMENT '企业ID',
  person_type VARCHAR(50) NULL COMMENT '人员类型',
  person_supplementary VARCHAR(100) NULL COMMENT '人员补充',
  house_type VARCHAR(50) NULL COMMENT '户籍类型',
  nationality VARCHAR(50) NULL COMMENT '国籍',
  education VARCHAR(100) NULL COMMENT '学历',
  job_intention VARCHAR(255) NULL COMMENT '求职意向',
  is_party_member INT NULL DEFAULT 0 COMMENT '是否党员',
  address VARCHAR(500) NULL COMMENT '地址',
  id_card_front VARCHAR(500) NULL COMMENT '身份证正面',
  id_card_back VARCHAR(500) NULL COMMENT '身份证背面',
  photo VARCHAR(500) NULL COMMENT '照片',
  edu_photo VARCHAR(500) NULL COMMENT '学历照片',
  work_contract VARCHAR(500) NULL COMMENT '劳动合同',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_user_info_tenant (tenant_id),
  INDEX idx_train_user_info_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 35. 档案管理 (train_archive) ====================
CREATE TABLE IF NOT EXISTS train_archive (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  archive_type VARCHAR(100) NULL COMMENT '档案类型',
  file_count INT NULL DEFAULT 0 COMMENT '文件数',
  archive_time VARCHAR(50) NULL COMMENT '归档时间',
  file_paths TEXT NULL COMMENT '文件路径JSON',
  status INT NULL DEFAULT 0 COMMENT '状态',
  approver_id BIGINT NULL COMMENT '审批人ID',
  approver_name VARCHAR(255) NULL COMMENT '审批人姓名',
  approve_time VARCHAR(50) NULL COMMENT '审批时间',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  remark TEXT NULL COMMENT '备注',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_archive_tenant (tenant_id),
  INDEX idx_train_archive_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 36. 考勤管理 (train_attendance) ====================
CREATE TABLE IF NOT EXISTS train_attendance (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  class_id BIGINT NULL COMMENT '班级ID',
  user_id BIGINT NULL COMMENT '用户ID',
  user_name VARCHAR(255) NULL COMMENT '用户名',
  attendance_date VARCHAR(50) NULL COMMENT '考勤日期',
  status INT NULL DEFAULT 0 COMMENT '状态 0-缺勤 1-出勤 2-迟到 3-早退',
  check_in_time VARCHAR(50) NULL COMMENT '签到时间',
  check_out_time VARCHAR(50) NULL COMMENT '签退时间',
  photo_path VARCHAR(500) NULL COMMENT '照片路径',
  video_path VARCHAR(500) NULL COMMENT '视频路径',
  remark VARCHAR(500) NULL COMMENT '备注',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  created_by BIGINT NULL COMMENT '创建人',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_attendance_tenant (tenant_id),
  INDEX idx_train_attendance_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 37. 排课管理 (train_schedule) ====================
CREATE TABLE IF NOT EXISTS train_schedule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class_id BIGINT NULL COMMENT '班级ID',
  course_id BIGINT NULL COMMENT '课程ID',
  teacher_id BIGINT NULL COMMENT '教师ID',
  teacher_name VARCHAR(255) NULL COMMENT '教师姓名',
  course_name VARCHAR(255) NULL COMMENT '课程名称',
  room_name VARCHAR(255) NULL COMMENT '教室名称',
  day_of_week INT NULL COMMENT '星期几',
  start_time VARCHAR(50) NULL COMMENT '开始时间',
  end_time VARCHAR(50) NULL COMMENT '结束时间',
  schedule_date VARCHAR(50) NULL COMMENT '日期',
  status INT NULL DEFAULT 1 COMMENT '状态',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  created_at DATETIME NULL COMMENT '创建时间',
  updated_at DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_schedule_tenant (tenant_id),
  INDEX idx_train_schedule_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 38. 结算管理 (train_settlement) ====================
CREATE TABLE IF NOT EXISTS train_settlement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  settlement_amount DECIMAL(16,2) NULL COMMENT '结算金额',
  institution_share DECIMAL(16,2) NULL COMMENT '机构分成',
  enterprise_share DECIMAL(16,2) NULL COMMENT '企业分成',
  third_party_share DECIMAL(16,2) NULL COMMENT '第三方分成',
  share_ratio VARCHAR(100) NULL COMMENT '分成比例',
  approval_status INT NULL DEFAULT 0 COMMENT '审批状态',
  approver_id BIGINT NULL COMMENT '审批人ID',
  approver_name VARCHAR(255) NULL COMMENT '审批人姓名',
  approve_time VARCHAR(50) NULL COMMENT '审批时间',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  remark TEXT NULL COMMENT '备注',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_settlement_tenant (tenant_id),
  INDEX idx_train_settlement_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 39. 补贴管理 (train_subsidy) ====================
CREATE TABLE IF NOT EXISTS train_subsidy (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  policy_template VARCHAR(255) NULL COMMENT '政策模板',
  subsidy_amount DECIMAL(16,2) NULL COMMENT '补贴金额',
  student_count INT NULL COMMENT '学员人数',
  material_package_path VARCHAR(500) NULL COMMENT '材料包路径',
  material_status INT NULL DEFAULT 0 COMMENT '材料状态',
  rssb_status INT NULL DEFAULT 0 COMMENT '人社局状态',
  rssb_submit_time VARCHAR(50) NULL COMMENT '人社局提交时间',
  rssb_receipt_no VARCHAR(255) NULL COMMENT '人社局回执号',
  rssb_reject_reason VARCHAR(500) NULL COMMENT '人社局驳回原因',
  granted_amount DECIMAL(16,2) NULL COMMENT '已拨付金额',
  grant_time VARCHAR(50) NULL COMMENT '拨付时间',
  remark TEXT NULL COMMENT '备注',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_subsidy_tenant (tenant_id),
  INDEX idx_train_subsidy_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 40. 总结管理 (train_summary) ====================
CREATE TABLE IF NOT EXISTS train_summary (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  certification_rate DECIMAL(10,2) NULL COMMENT '认定通过率',
  subsidy_success_rate DECIMAL(10,2) NULL COMMENT '补贴成功率',
  achievements TEXT NULL COMMENT '成果',
  lessons_learned TEXT NULL COMMENT '经验教训',
  improvement_suggestions TEXT NULL COMMENT '改进建议',
  content TEXT NULL COMMENT '内容',
  file_path VARCHAR(500) NULL COMMENT '文件路径',
  approval_status INT NULL DEFAULT 0 COMMENT '审批状态',
  approver_id BIGINT NULL COMMENT '审批人ID',
  approver_name VARCHAR(255) NULL COMMENT '审批人姓名',
  approve_time VARCHAR(50) NULL COMMENT '审批时间',
  reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
  status INT NULL DEFAULT 0 COMMENT '状态',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_summary_tenant (tenant_id),
  INDEX idx_train_summary_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 41. 班级规则 (train_class_rule) ====================
CREATE TABLE IF NOT EXISTS train_class_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NULL COMMENT '项目ID',
  rule_type VARCHAR(100) NULL COMMENT '规则类型',
  rule_config TEXT NULL COMMENT '规则配置JSON',
  max_students INT NULL COMMENT '最大学生数',
  status INT NULL DEFAULT 1 COMMENT '状态',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  created_at DATETIME NULL COMMENT '创建时间',
  updated_at DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_train_class_rule_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
