-- V20260610_001: 组卷计划主表 + 明细表
-- 对标 gjjd-java exam_make_exam_plan / exam_make_exam_plan_detail

CREATE TABLE IF NOT EXISTS exam_make_exam_plan (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  course_id INT NULL COMMENT '科目ID → exam_course.id',
  rate_id INT NULL COMMENT '比重ID → exam_knowledge_rate.id',
  type INT NULL DEFAULT 0 COMMENT '类型',
  exam_type INT NOT NULL DEFAULT 0 COMMENT '考试类型: 0=理论, 1=实操',
  title VARCHAR(255) NULL COMMENT '名称',
  time INT NULL COMMENT '考试时长(分钟)',
  is_computer INT NULL DEFAULT 0 COMMENT '是否机考: 0=否, 1=是',
  is_print INT NULL DEFAULT 0 COMMENT '是否打印: 0=否, 1=是',
  open_rate INT NULL DEFAULT 100 COMMENT '公开比例(%)',
  total_point INT NULL DEFAULT 100 COMMENT '总分值',
  pass_point INT NULL DEFAULT 60 COMMENT '及格分值',
  comments VARCHAR(500) NULL COMMENT '备注',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  is_all INT NULL DEFAULT 0 COMMENT '是否选择全部题目: 0=否, 1=是',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_make_plan_course (tenant_id, course_id),
  INDEX idx_make_plan_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组卷计划';

CREATE TABLE IF NOT EXISTS exam_make_exam_plan_detail (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  make_id INT NOT NULL COMMENT '组卷计划ID → exam_make_exam_plan.id',
  course_id INT NULL COMMENT '科目ID',
  type VARCHAR(50) NULL COMMENT '试题类型: single/multi/judge/fill/essay/short/explain/cal/simple_fill/answer/analysis',
  num INT NULL DEFAULT 0 COMMENT '数量',
  point DECIMAL(10,2) NULL DEFAULT 0 COMMENT '每题分值',
  start_point INT NULL COMMENT '参考分值(起)',
  end_point INT NULL COMMENT '参考分值(止)',
  difficulty_start INT NULL COMMENT '难度(起): 0=易~4=难',
  difficulty_end INT NULL COMMENT '难度(止): 0=易~4=难',
  total_point DECIMAL(10,2) NULL COMMENT '总分值 = num × point',
  user_id INT NULL COMMENT '创建人',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_plan_detail_make (tenant_id, make_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组卷计划明细';
