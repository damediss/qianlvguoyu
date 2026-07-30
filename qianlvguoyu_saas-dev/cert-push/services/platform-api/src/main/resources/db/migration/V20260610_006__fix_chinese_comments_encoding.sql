-- V20260610_006: 修复中文注释乱码（客户端字符集问题）
SET NAMES utf8mb4;

-- ===== exam_make_exam_plan =====
ALTER TABLE exam_make_exam_plan
  COMMENT = '组卷计划',
  MODIFY course_id INT NULL COMMENT '科目ID → exam_course.id',
  MODIFY rate_id INT NULL COMMENT '比重ID → exam_knowledge_rate.id',
  MODIFY type INT NULL DEFAULT 0 COMMENT '类型',
  MODIFY exam_type INT NOT NULL DEFAULT 0 COMMENT '考试类型: 0=理论, 1=实操',
  MODIFY title VARCHAR(255) NULL COMMENT '名称',
  MODIFY time INT NULL COMMENT '考试时长(分钟)',
  MODIFY is_computer INT NULL DEFAULT 0 COMMENT '是否机考: 0=否, 1=是',
  MODIFY is_print INT NULL DEFAULT 0 COMMENT '是否打印: 0=否, 1=是',
  MODIFY open_rate INT NULL DEFAULT 100 COMMENT '公开比例(%)',
  MODIFY total_point INT NULL DEFAULT 100 COMMENT '总分值',
  MODIFY pass_point INT NULL DEFAULT 60 COMMENT '及格分值',
  MODIFY comments VARCHAR(500) NULL COMMENT '备注',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY is_all INT NULL DEFAULT 0 COMMENT '是否选择全部题目: 0=否, 1=是',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_make_exam_plan_detail =====
ALTER TABLE exam_make_exam_plan_detail
  COMMENT = '组卷计划明细',
  MODIFY make_id INT NOT NULL COMMENT '组卷计划ID → exam_make_exam_plan.id',
  MODIFY course_id INT NULL COMMENT '科目ID',
  MODIFY type VARCHAR(50) NULL COMMENT '试题类型: single/multi/judge/fill/essay/short/explain/cal/simple_fill/answer/analysis',
  MODIFY num INT NULL DEFAULT 0 COMMENT '数量',
  MODIFY point DECIMAL(10,2) NULL DEFAULT 0 COMMENT '每题分值',
  MODIFY start_point INT NULL COMMENT '参考分值(起)',
  MODIFY end_point INT NULL COMMENT '参考分值(止)',
  MODIFY difficulty_start INT NULL COMMENT '难度(起): 0=易~4=难',
  MODIFY difficulty_end INT NULL COMMENT '难度(止): 0=易~4=难',
  MODIFY total_point DECIMAL(10,2) NULL COMMENT '总分值 = num × point',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_knowledge_rate =====
ALTER TABLE exam_knowledge_rate
  COMMENT = '知识比重',
  MODIFY course_id INT NULL COMMENT '科目ID → exam_course.id',
  MODIFY type INT NULL DEFAULT 0 COMMENT '类型',
  MODIFY is_standard INT NULL DEFAULT 0 COMMENT '是否标准比重: 0=否, 1=是',
  MODIFY title VARCHAR(255) NULL COMMENT '标题',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号(越小越靠前)',
  MODIFY comments VARCHAR(500) NULL COMMENT '备注',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=冻结, 1=正常',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY deleted INT NULL DEFAULT 0 COMMENT '逻辑删除: 0=否, 1=是',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_knowledge_rate_detail =====
ALTER TABLE exam_knowledge_rate_detail
  COMMENT = '知识比重详情',
  MODIFY knowledge_rate_id INT NOT NULL COMMENT '比重ID → exam_knowledge_rate.id',
  MODIFY knowledge_id INT NULL COMMENT '知识点ID → exam_knowledge_structure.id',
  MODIFY rate INT NULL DEFAULT 0 COMMENT '比重百分比(%)',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY comments VARCHAR(500) NULL COMMENT '备注',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=冻结, 1=正常',
  MODIFY deleted INT NULL DEFAULT 0 COMMENT '逻辑删除: 0=否, 1=是',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_paper =====
ALTER TABLE exam_paper
  COMMENT = '试卷',
  MODIFY make_id INT NULL COMMENT '关联组卷计划ID → exam_make_exam_plan.id',
  MODIFY source INT NULL DEFAULT 0 COMMENT '来源: 0=默认',
  MODIFY type INT NULL DEFAULT 0 COMMENT '类型',
  MODIFY code VARCHAR(100) NULL COMMENT '试卷编号',
  MODIFY title VARCHAR(255) NULL COMMENT '名称',
  MODIFY time INT NULL COMMENT '考试时间(分钟)',
  MODIFY total_point INT NULL DEFAULT 100 COMMENT '总分',
  MODIFY pass_point INT NULL DEFAULT 60 COMMENT '及格分',
  MODIFY is_computer INT NULL DEFAULT 0 COMMENT '是否机考: 0=否, 1=是',
  MODIFY is_print INT NULL DEFAULT 0 COMMENT '是否打印: 0=否, 1=是',
  MODIFY comments VARCHAR(500) NULL COMMENT '描述',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_paper_detail =====
ALTER TABLE exam_paper_detail
  COMMENT = '试卷详情',
  MODIFY paper_id INT NOT NULL COMMENT '试卷ID → exam_paper.id',
  MODIFY exam_id INT NULL COMMENT '题库试题ID → exam_exam_detail.id',
  MODIFY type VARCHAR(50) NULL COMMENT '试题类型',
  MODIFY diff INT NULL COMMENT '难度: 0=易~4=难',
  MODIFY content TEXT NULL COMMENT '题干',
  MODIFY answer TEXT NULL COMMENT '答案',
  MODIFY score DECIMAL(10,2) NULL COMMENT '分值',
  MODIFY resolve TEXT NULL COMMENT '解析',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_operate_paper =====
ALTER TABLE exam_operate_paper
  COMMENT = '实操试卷',
  MODIFY make_id INT NULL COMMENT '关联组卷计划ID',
  MODIFY source INT NULL DEFAULT 0 COMMENT '来源',
  MODIFY work_type_id INT NULL COMMENT '工种ID',
  MODIFY course_id INT NULL COMMENT '科目ID',
  MODIFY num INT NULL DEFAULT 0 COMMENT '总题数',
  MODIFY code VARCHAR(100) NULL COMMENT '编号',
  MODIFY level VARCHAR(50) NULL COMMENT '等级名(初级工/五级~首席技师)',
  MODIFY title VARCHAR(255) NULL COMMENT '名称',
  MODIFY time INT NULL COMMENT '考试时间(分钟)',
  MODIFY total_point INT NULL DEFAULT 100 COMMENT '总分',
  MODIFY pass_point INT NULL DEFAULT 60 COMMENT '及格分',
  MODIFY comments VARCHAR(500) NULL COMMENT '描述',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ===== exam_operate_paper_detail =====
ALTER TABLE exam_operate_paper_detail
  COMMENT = '实操试卷详情',
  MODIFY paper_id INT NOT NULL COMMENT '实操试卷ID → exam_operate_paper.id',
  MODIFY title VARCHAR(255) NULL COMMENT '标题',
  MODIFY student_prepare TEXT NULL COMMENT '考生准备',
  MODIFY content TEXT NULL COMMENT '内容',
  MODIFY score DECIMAL(10,2) NULL COMMENT '配分',
  MODIFY weight INT NULL COMMENT '权重',
  MODIFY equip_prepare TEXT NULL COMMENT '设备设施及准备',
  MODIFY exam_content TEXT NULL COMMENT '考试内容',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';

-- ===== exam_operate_detail =====
ALTER TABLE exam_operate_detail
  COMMENT = '实操试题',
  MODIFY course_id INT NULL COMMENT '科目ID',
  MODIFY work_type_id INT NULL COMMENT '工种ID',
  MODIFY knowledge_id INT NULL COMMENT '知识点ID',
  MODIFY title VARCHAR(255) NULL COMMENT '标题',
  MODIFY student_prepare TEXT NULL COMMENT '考生准备',
  MODIFY content TEXT NULL COMMENT '内容',
  MODIFY score DECIMAL(10,2) NULL COMMENT '配分',
  MODIFY weight INT NULL COMMENT '权重',
  MODIFY equip_prepare TEXT NULL COMMENT '设备设施及准备',
  MODIFY exam_content TEXT NULL COMMENT '考试内容',
  MODIFY user_id INT NULL COMMENT '创建人',
  MODIFY status INT NULL DEFAULT 1 COMMENT '状态: 0=无效, 1=有效',
  MODIFY sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  MODIFY tenant_id INT NOT NULL COMMENT '租户ID',
  MODIFY create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';

