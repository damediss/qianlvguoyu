-- ============================================================
-- 补充 PHP 遗留系统中存在但 Java 项目缺失的培训管理表
-- 包含：报名管理、考试编排、经费明细、文件模板、企业需求等
-- ============================================================

-- ==================== 1. 经费明细 (train_solution_badge) ====================
CREATE TABLE IF NOT EXISTS train_solution_badge (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  solution_id BIGINT NOT NULL COMMENT '方案ID',
  title VARCHAR(255) NULL COMMENT '费用项目名称',
  badge DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '费用金额',
  expend DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '支出金额',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_solution_badge_solution (solution_id),
  INDEX idx_solution_badge_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 2. 考试场次 (train_exam_session) ====================
CREATE TABLE IF NOT EXISTS train_exam_session (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  course_id BIGINT NULL COMMENT '课程ID',
  user_id BIGINT NULL COMMENT '用户ID',
  start_time VARCHAR(20) NULL COMMENT '开始时间',
  end_time VARCHAR(20) NULL COMMENT '结束时间',
  course_name VARCHAR(255) NULL COMMENT '课程名称',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_session_exam (exam_id),
  INDEX idx_exam_session_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 3. 考场配置 (train_exam_room_list) ====================
CREATE TABLE IF NOT EXISTS train_exam_room_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT NULL COMMENT '组织ID',
  org_name VARCHAR(255) NULL COMMENT '组织名称',
  type VARCHAR(50) NULL COMMENT '考场类型',
  room_name VARCHAR(255) NULL COMMENT '考场名称',
  position VARCHAR(255) NULL COMMENT '位置',
  max_num INT NULL DEFAULT 0 COMMENT '最大容纳人数',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_room_org (org_id),
  INDEX idx_exam_room_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 4. 考生座位分配 (train_exam_people_in_room) ====================
CREATE TABLE IF NOT EXISTS train_exam_people_in_room (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  course_id BIGINT NULL COMMENT '课程ID',
  session_id BIGINT NULL COMMENT '场次ID',
  room_id BIGINT NULL COMMENT '考场ID',
  exam_room_id BIGINT NULL COMMENT '考场关联ID',
  user_id BIGINT NULL COMMENT '用户ID',
  realname VARCHAR(100) NULL COMMENT '姓名',
  people_id BIGINT NULL COMMENT '考生人员ID(关联train_exam_people_list)',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_people_room_exam (exam_id),
  INDEX idx_people_room_room (room_id),
  INDEX idx_people_room_session (session_id),
  INDEX idx_people_room_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 5. 考务人员分配 (train_exam_staff) ====================
CREATE TABLE IF NOT EXISTS train_exam_staff (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  exam_room_id BIGINT NULL COMMENT '考场ID',
  room_id BIGINT NULL COMMENT '教室ID',
  user_id BIGINT NULL COMMENT '用户ID',
  realname VARCHAR(100) NULL COMMENT '姓名',
  type VARCHAR(50) NULL COMMENT '人员类型(监考/考评/督导/考务)',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_staff_exam (exam_id),
  INDEX idx_exam_staff_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 6. 考务人员配置库 (train_exam_staff_config) ====================
CREATE TABLE IF NOT EXISTS train_exam_staff_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(50) NULL COMMENT '人员类型',
  realname VARCHAR(100) NULL COMMENT '姓名',
  user_id BIGINT NULL COMMENT '用户ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_staff_config_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 7. 试卷关联 (train_exam_shijuan) ====================
CREATE TABLE IF NOT EXISTS train_exam_shijuan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  paper_id BIGINT NOT NULL COMMENT '试卷ID',
  exam_room_id BIGINT NULL COMMENT '考场ID',
  operate_paper_id BIGINT NULL COMMENT '实操试卷ID',
  room_id BIGINT NULL COMMENT '教室ID',
  user_id BIGINT NULL COMMENT '用户ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_shijuan_exam (exam_id),
  INDEX idx_exam_shijuan_paper (paper_id),
  INDEX idx_exam_shijuan_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 8. 考试工种关联 (train_exam_work_type_list) ====================
CREATE TABLE IF NOT EXISTS train_exam_work_type_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  work_type_id BIGINT NULL COMMENT '工种ID',
  course_id BIGINT NULL COMMENT '课程ID',
  level VARCHAR(50) NULL COMMENT '等级',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_work_type_exam (exam_id),
  INDEX idx_exam_work_type_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 9. 考试复习配置 (train_exam_review) ====================
CREATE TABLE IF NOT EXISTS train_exam_review (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  paper_id BIGINT NULL COMMENT '试卷ID',
  user_id BIGINT NULL COMMENT '用户ID',
  num INT NULL DEFAULT 0 COMMENT '复习题数',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_review_exam (exam_id),
  INDEX idx_exam_review_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 10. 自主报名计划 (train_sign_up_self) ====================
CREATE TABLE IF NOT EXISTS train_sign_up_self (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NULL COMMENT '标题',
  cover VARCHAR(500) NULL COMMENT '封面图',
  train_time VARCHAR(255) NULL COMMENT '培训时间',
  exam_time VARCHAR(255) NULL COMMENT '考试时间',
  org_id BIGINT NULL COMMENT '组织ID',
  train_info_id BIGINT NULL COMMENT '培训信息ID',
  sort_number INT NULL DEFAULT 0 COMMENT '排序号',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_sign_up_self_org (org_id),
  INDEX idx_sign_up_self_train_info (train_info_id),
  INDEX idx_sign_up_self_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 11. 报名人员 (train_sign_up_people_self) ====================
CREATE TABLE IF NOT EXISTS train_sign_up_people_self (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sign_up_id BIGINT NOT NULL COMMENT '报名计划ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  name VARCHAR(100) NULL COMMENT '姓名',
  phone VARCHAR(20) NULL COMMENT '手机号',
  id_card_number VARCHAR(20) NULL COMMENT '身份证号',
  gender VARCHAR(10) NULL COMMENT '性别',
  education VARCHAR(50) NULL COMMENT '学历',
  photo VARCHAR(500) NULL COMMENT '照片路径',
  contract VARCHAR(500) NULL COMMENT '合同路径',
  certificate VARCHAR(500) NULL COMMENT '证书路径',
  status INT NULL DEFAULT 0 COMMENT '状态',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_sign_up_people_sign_up (sign_up_id),
  INDEX idx_sign_up_people_user (platform_user_id),
  INDEX idx_sign_up_people_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 12. OCR识别记录 (train_sign_up_orc_self) ====================
CREATE TABLE IF NOT EXISTS train_sign_up_orc_self (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sign_up_id BIGINT NULL COMMENT '报名计划ID',
  platform_user_id BIGINT NULL COMMENT '平台用户ID',
  orc_data TEXT NULL COMMENT 'OCR识别原始数据',
  status INT NULL DEFAULT 0 COMMENT '状态 0-失败 1-成功',
  fail_reason VARCHAR(500) NULL COMMENT '失败原因',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_sign_up_orc_sign_up (sign_up_id),
  INDEX idx_sign_up_orc_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 13. 课堂任务日志 (train_task_log) ====================
CREATE TABLE IF NOT EXISTS train_task_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  train_class_id BIGINT NULL COMMENT '班级ID',
  ck_id BIGINT NULL COMMENT '任务检查点ID',
  date VARCHAR(50) NULL COMMENT '日期',
  addtime INT NULL COMMENT '添加时间戳',
  status INT NULL DEFAULT 0 COMMENT '状态',
  times VARCHAR(100) NULL COMMENT '时间范围',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_task_log_class (train_class_id),
  INDEX idx_task_log_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 14. 文件模板 (train_file_temp) ====================
CREATE TABLE IF NOT EXISTS train_file_temp (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(50) NULL COMMENT '模板类型',
  title VARCHAR(255) NULL COMMENT '模板名称',
  city VARCHAR(100) NULL COMMENT '城市/地区',
  path_source VARCHAR(500) NULL COMMENT '源文件路径',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_file_temp_type (type),
  INDEX idx_file_temp_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 15. 基础配置数据 (train_base_data) ====================
CREATE TABLE IF NOT EXISTS train_base_data (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  data_key VARCHAR(100) NULL COMMENT '配置键',
  data_value TEXT NULL COMMENT '配置值',
  data_type VARCHAR(50) NULL COMMENT '数据类型',
  description VARCHAR(500) NULL COMMENT '描述',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_base_data_key (data_key),
  INDEX idx_base_data_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 16. 岗位-工种映射 (train_position_to_work_type) ====================
CREATE TABLE IF NOT EXISTS train_position_to_work_type (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  position_name VARCHAR(255) NULL COMMENT '岗位名称',
  work_type_id BIGINT NULL COMMENT '工种ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_position_work_type (work_type_id),
  INDEX idx_position_work_type_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 17. 企业培训需求 (demand_train) ====================
CREATE TABLE IF NOT EXISTS demand_train (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT NOT NULL COMMENT '企业ID',
  content TEXT NULL COMMENT '需求内容',
  nums INT NULL DEFAULT 0 COMMENT '人数',
  status INT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-已处理',
  addtime INT NULL COMMENT '添加时间戳',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_demand_train_org (org_id),
  INDEX idx_demand_train_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 18. 企业人才需求 (demand_employee) ====================
CREATE TABLE IF NOT EXISTS demand_employee (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_id BIGINT NOT NULL COMMENT '企业ID',
  work VARCHAR(255) NULL COMMENT '工种/岗位',
  education VARCHAR(50) NULL COMMENT '学历要求',
  major VARCHAR(255) NULL COMMENT '专业要求',
  addtime INT NULL COMMENT '添加时间戳',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_demand_employee_org (org_id),
  INDEX idx_demand_employee_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 19. 考试人员列表 (train_exam_people_list) ====================
CREATE TABLE IF NOT EXISTS train_exam_people_list (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL COMMENT '考试计划ID',
  platform_user_id BIGINT NOT NULL COMMENT '平台用户ID',
  work_type_id BIGINT NULL COMMENT '工种ID',
  work_type_level VARCHAR(50) NULL COMMENT '工种等级',
  realname VARCHAR(100) NULL COMMENT '姓名',
  id_card_num VARCHAR(50) NULL COMMENT '身份证号',
  phone VARCHAR(20) NULL COMMENT '电话',
  education VARCHAR(50) NULL COMMENT '学历',
  room_id BIGINT NULL COMMENT '考场ID',
  in_white_list INT NULL DEFAULT 0 COMMENT '是否在白名单',
  normal_score DECIMAL(5,2) NULL COMMENT '理论成绩',
  score DECIMAL(5,2) NULL COMMENT '实操成绩',
  total_score DECIMAL(5,2) NULL COMMENT '总成绩',
  learn_time INT NULL DEFAULT 0 COMMENT '学习次数',
  learn_use_time INT NULL DEFAULT 0 COMMENT '学习使用时间(秒)',
  user_id BIGINT NULL COMMENT '用户ID',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_exam_people_exam (exam_id),
  INDEX idx_exam_people_platform_user (platform_user_id),
  INDEX idx_exam_people_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 20. 课程-课件补充字段 (course_kejian) ====================
-- 补充遗漏的字段（如果表已存在则追加）
-- ALTER TABLE course_kejian ADD COLUMN IF NOT EXISTS jigou_id BIGINT NULL COMMENT '机构ID' AFTER tag;
-- ALTER TABLE course_kejian ADD COLUMN IF NOT EXISTS owner_id BIGINT NULL COMMENT '所有者ID' AFTER jigou_id;
-- ALTER TABLE course_kejian ADD COLUMN IF NOT EXISTS addtime INT NULL COMMENT '添加时间戳' AFTER owner_id;

-- ==================== 21. 预算表补充字段 (train_budget) ====================
-- 补齐 PHP 中的详细收支字段
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS province VARCHAR(100) NULL COMMENT '省份' AFTER train_id;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL COMMENT '城市' AFTER province;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS region VARCHAR(100) NULL COMMENT '区域' AFTER city;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS bonus_standard VARCHAR(100) NULL COMMENT '补贴标准' AFTER train_level;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS expect_income DECIMAL(12,2) NULL COMMENT '预期收入' AFTER bonus_standard;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS company_require_commission DECIMAL(12,2) NULL COMMENT '企业要求佣金' AFTER research_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS agent_require_commission DECIMAL(12,2) NULL COMMENT '代理要求佣金' AFTER company_require_commission;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS teacher_course_fee DECIMAL(12,2) NULL COMMENT '教师课时费' AFTER agent_require_commission;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS teacher_fee DECIMAL(12,2) NULL COMMENT '教师费用' AFTER teacher_course_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS staff_fee DECIMAL(12,2) NULL COMMENT '员工费用' AFTER teacher_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS staff_travel_fee DECIMAL(12,2) NULL COMMENT '员工差旅费' AFTER staff_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS train_fee DECIMAL(12,2) NULL COMMENT '培训费' AFTER staff_travel_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS point_rewards DECIMAL(12,2) NULL COMMENT '积分奖励' AFTER train_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS business_fee DECIMAL(12,2) NULL COMMENT '业务费' AFTER point_rewards;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS charge_fee VARCHAR(100) NULL COMMENT '收费方式' AFTER people_num;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS service_fee DECIMAL(12,2) NULL COMMENT '服务费' AFTER charge_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS review_fee DECIMAL(12,2) NULL COMMENT '评审费' AFTER service_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS maintain_fee DECIMAL(12,2) NULL COMMENT '维护费' AFTER review_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS makeup_people_num INT NULL COMMENT '补考人数' AFTER maintain_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS theory_makeup_people_num INT NULL COMMENT '理论补考人数' AFTER makeup_people_num;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS operation_makeup_people_num INT NULL COMMENT '实操补考人数' AFTER theory_makeup_people_num;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS staff_makeup_train_fee DECIMAL(12,2) NULL COMMENT '员工补考培训费' AFTER operation_makeup_people_num;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS review_makeup_train_fee DECIMAL(12,2) NULL COMMENT '评审补考培训费' AFTER staff_makeup_train_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS profit DECIMAL(12,2) NULL COMMENT '利润' AFTER review_makeup_train_fee;
-- ALTER TABLE train_budget ADD COLUMN IF NOT EXISTS comments VARCHAR(1000) NULL COMMENT '备注' AFTER profit;
