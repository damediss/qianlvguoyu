-- V20260610_003: DROP 旧 exam_paper 表并重建为 gjjd-java 兼容结构
-- 旧表无 Java/前端代码引用，可安全删除

-- 1. 删除旧表
DROP TABLE IF EXISTS exam_paper_question;
DROP TABLE IF EXISTS exam_paper;

-- 2. 重建 exam_paper (理论试卷, gjjd-java 兼容)
CREATE TABLE exam_paper (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  make_id INT NULL COMMENT '关联组卷计划ID → exam_make_exam_plan.id',
  source INT NULL DEFAULT 0 COMMENT '来源: 0=默认',
  type INT NULL DEFAULT 0 COMMENT '类型',
  code VARCHAR(100) NULL COMMENT '试卷编号',
  title VARCHAR(255) NULL COMMENT '名称',
  time INT NULL COMMENT '考试时间(分钟)',
  total_point INT NULL DEFAULT 100 COMMENT '总分',
  pass_point INT NULL DEFAULT 60 COMMENT '及格分',
  is_computer INT NULL DEFAULT 0 COMMENT '是否机考: 0=否, 1=是',
  is_print INT NULL DEFAULT 0 COMMENT '是否打印: 0=否, 1=是',
  comments VARCHAR(500) NULL COMMENT '描述',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_paper_make (tenant_id, make_id),
  INDEX idx_paper_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷';

-- 3. exam_paper_detail (理论试卷详情 - 抽题结果)
CREATE TABLE IF NOT EXISTS exam_paper_detail (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  paper_id INT NOT NULL COMMENT '试卷ID → exam_paper.id',
  exam_id INT NULL COMMENT '题库试题ID → exam_exam_detail.id',
  type VARCHAR(50) NULL COMMENT '试题类型',
  diff INT NULL COMMENT '难度: 0=易~4=难',
  content TEXT NULL COMMENT '题干',
  answer TEXT NULL COMMENT '答案',
  score DECIMAL(10,2) NULL COMMENT '分值',
  resolve TEXT NULL COMMENT '解析',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_paper_detail_paper (tenant_id, paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷详情';

-- 4. exam_operate_paper (实操试卷)
CREATE TABLE IF NOT EXISTS exam_operate_paper (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  make_id INT NULL COMMENT '关联组卷计划ID',
  source INT NULL DEFAULT 0 COMMENT '来源',
  work_type_id INT NULL COMMENT '工种ID',
  course_id INT NULL COMMENT '科目ID',
  num INT NULL DEFAULT 0 COMMENT '总题数',
  code VARCHAR(100) NULL COMMENT '编号',
  level VARCHAR(50) NULL COMMENT '等级名(初级工/五级~首席技师)',
  title VARCHAR(255) NULL COMMENT '名称',
  time INT NULL COMMENT '考试时间(分钟)',
  total_point INT NULL DEFAULT 100 COMMENT '总分',
  pass_point INT NULL DEFAULT 60 COMMENT '及格分',
  comments VARCHAR(500) NULL COMMENT '描述',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_operate_paper_make (tenant_id, make_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实操试卷';

-- 5. exam_operate_paper_detail (实操试卷详情)
CREATE TABLE IF NOT EXISTS exam_operate_paper_detail (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  paper_id INT NOT NULL COMMENT '实操试卷ID → exam_operate_paper.id',
  title VARCHAR(255) NULL COMMENT '标题',
  student_prepare TEXT NULL COMMENT '考生准备',
  content TEXT NULL COMMENT '内容',
  score DECIMAL(10,2) NULL COMMENT '配分',
  weight INT NULL COMMENT '权重',
  equip_prepare TEXT NULL COMMENT '设备设施及准备',
  exam_content TEXT NULL COMMENT '考试内容',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  INDEX idx_operate_detail_paper (tenant_id, paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实操试卷详情';

-- 6. exam_operate_detail (实操试题 - 实操抽题来源)
CREATE TABLE IF NOT EXISTS exam_operate_detail (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  course_id INT NULL COMMENT '科目ID',
  work_type_id INT NULL COMMENT '工种ID',
  knowledge_id INT NULL COMMENT '知识点ID',
  title VARCHAR(255) NULL COMMENT '标题',
  student_prepare TEXT NULL COMMENT '考生准备',
  content TEXT NULL COMMENT '内容',
  score DECIMAL(10,2) NULL COMMENT '配分',
  weight INT NULL COMMENT '权重',
  equip_prepare TEXT NULL COMMENT '设备设施及准备',
  exam_content TEXT NULL COMMENT '考试内容',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  INDEX idx_operate_detail_course (tenant_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实操试题';
