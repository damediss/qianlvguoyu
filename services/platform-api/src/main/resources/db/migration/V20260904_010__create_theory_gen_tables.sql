-- 理论知识试题生成管理：细目表批次 + 节点 + 生成批次 + 试题草稿（4 张新表）
-- 方案：docs/04_requirements/理论知识试题生成管理方案.md v1.2（T1 迁移 1）
-- 版本号依据（2026-09-04 双库查询）：gjjddbpro(生产) MAX=20260831.001；
-- gjjddb2026(测试) 已被 train_certificate 工作流占用 20260904.001/.002，
-- 故本组迁移取 20260904.010~013，避开两库已用号及 .003~.009 顺延区间（踩坑 #7）。
-- 原 20260904_001~004 编号从未在任何库应用，改号无副作用。

-- 1. 细目表批次表
CREATE TABLE IF NOT EXISTS exam_theory_blueprint (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  work_type_id INT NOT NULL COMMENT '工种 ID exam_work_type.id',
  level TINYINT NOT NULL COMMENT '等级 1初级工 2中级工 3高级工 4技师 5高级技师（与 task_exam.level 同口径）',
  course_id INT NULL COMMENT '挂接科目 exam_course.id（导入时匹配、入库时兜底创建）',
  title VARCHAR(255) NOT NULL COMMENT '细目表标题，如 氧化铝制取工（五级/初级工）轻冶料浆配料工',
  source_file_name VARCHAR(255) NULL COMMENT '上传文件名',
  point_count INT NOT NULL DEFAULT 0 COMMENT '考评点数量',
  xyz_ratio VARCHAR(20) NULL COMMENT '考评点重要程度比例，如 181:12:7',
  weight_check TINYINT NOT NULL DEFAULT 0 COMMENT '权重合计校验 0未通过 1通过',
  check_report JSON NULL COMMENT '导入校验报告',
  status VARCHAR(32) NOT NULL DEFAULT 'imported' COMMENT 'imported已导入 generating生成中 done已完成',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_etb_tenant (tenant_id),
  KEY idx_etb_work_level (tenant_id, work_type_id, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='理论知识考评要素细目表批次';

-- 2. 细目表节点表（一/二/三级考评范围 + 考评点）
CREATE TABLE IF NOT EXISTS exam_theory_blueprint_node (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  blueprint_id BIGINT NOT NULL COMMENT '批次 exam_theory_blueprint.id',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点 id，0=根',
  node_level TINYINT NOT NULL COMMENT '1一级 2二级 3三级 4考评点',
  code VARCHAR(20) NOT NULL COMMENT '本级代码 A/B/001',
  full_code VARCHAR(50) NOT NULL COMMENT '全路径代码，如 A-A-A-001（考评点）',
  name VARCHAR(500) NOT NULL COMMENT '名称',
  weight DECIMAL(5,2) NULL COMMENT '考评权重（百分比数值 0-100）',
  importance CHAR(1) NULL COMMENT '重要程度 X/Y/Z（仅考评点）',
  related_knowledge TEXT NULL COMMENT '相关知识要求（仅三级）',
  xyz_stat VARCHAR(20) NULL COMMENT '累计重要程度比例，如 80:7:3（一/二/三级）',
  knowledge_id BIGINT NULL COMMENT '打通回填 exam_knowledge_structure.id',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_etbn_blueprint (tenant_id, blueprint_id),
  KEY idx_etbn_parent (tenant_id, parent_id),
  KEY idx_etbn_full_code (tenant_id, blueprint_id, full_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='理论知识细目表节点';

-- 3. 生成批次表
CREATE TABLE IF NOT EXISTS exam_theory_gen_batch (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  blueprint_id BIGINT NOT NULL COMMENT '细目表批次 exam_theory_blueprint.id',
  model_config_id BIGINT NOT NULL COMMENT '模型配置 exam_faq_model_config.id',
  model_id VARCHAR(128) NOT NULL COMMENT '模型 ID 快照',
  model_code VARCHAR(64) NULL COMMENT '模型编码快照',
  thinking VARCHAR(16) NULL COMMENT '思考模式快照 enabled/disabled',
  reasoning_effort VARCHAR(8) NULL COMMENT '推理强度快照 low/high/max',
  quota_config JSON NOT NULL COMMENT '按题型每考评点题量配置',
  status VARCHAR(32) NOT NULL DEFAULT 'pending'
    COMMENT 'pending/running/paused/done/failed',
  total_points INT NOT NULL DEFAULT 0 COMMENT '考评点总数',
  done_points INT NOT NULL DEFAULT 0 COMMENT '已完成考评点数',
  failed_points INT NOT NULL DEFAULT 0 COMMENT '失败考评点数',
  total_questions INT NOT NULL DEFAULT 0 COMMENT '已生成试题数',
  token_usage BIGINT NOT NULL DEFAULT 0 COMMENT 'token 用量累计',
  prompt_digest VARCHAR(200) NULL COMMENT 'prompt 摘要',
  fail_reason VARCHAR(500) NULL COMMENT '失败原因',
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_etgb_tenant (tenant_id),
  KEY idx_etgb_blueprint (tenant_id, blueprint_id),
  KEY idx_etgb_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='理论知识试题生成批次';

-- 4. 试题草稿表
CREATE TABLE IF NOT EXISTS exam_theory_gen_draft (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  batch_id BIGINT NOT NULL COMMENT '生成批次 exam_theory_gen_batch.id',
  blueprint_id BIGINT NOT NULL COMMENT '细目表批次 exam_theory_blueprint.id',
  node_id BIGINT NOT NULL COMMENT '考评点 exam_theory_blueprint_node.id',
  knowledge_id BIGINT NULL COMMENT '打通回填 exam_knowledge_structure.id',
  question_type VARCHAR(20) NOT NULL COMMENT 'single/multi/judge/essay/cal/draw/discuss/case_analysis',
  type_code CHAR(1) NOT NULL COMMENT '指引题型代码 B单选 C判断 D多选 E简答计算绘图 F论述 I案例',
  difficulty TINYINT NOT NULL DEFAULT 3 COMMENT '难度 1易 2较易 3中 4较难 5难',
  consistency TINYINT NOT NULL DEFAULT 5 COMMENT '题目-目标一致性 1-5，默认 5',
  content TEXT NOT NULL COMMENT '题干',
  options JSON NULL COMMENT '客观题选项 [{label,content}]',
  answer VARCHAR(500) NOT NULL COMMENT '客观题答案字母或判断题√/×',
  analysis TEXT NULL COMMENT '解析',
  reference_answer TEXT NULL COMMENT '主观题参考答案',
  score_points TEXT NULL COMMENT '主观题评分要点',
  scoring_standard TEXT NULL COMMENT '主观题评分标准',
  score DECIMAL(5,1) NULL COMMENT '建议分值（5/10/15 档）',
  ai_model VARCHAR(100) NULL COMMENT '生成模型',
  ai_thinking VARCHAR(16) NULL COMMENT '生成时思考模式',
  gen_duration_ms INT NULL COMMENT '生成耗时毫秒',
  review_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/approved/rejected',
  review_by BIGINT NULL,
  review_at DATETIME NULL,
  publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未入库 1已入库',
  published_task_type VARCHAR(10) NULL COMMENT 'exam/examsub',
  published_task_id BIGINT NULL COMMENT 'task_exam / task_examsub.id',
  published_detail_id BIGINT NULL COMMENT 'task_exam_detail / task_examsub_detail.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_etgd_batch (tenant_id, batch_id),
  KEY idx_etgd_node (tenant_id, node_id),
  KEY idx_etgd_review (tenant_id, batch_id, review_status),
  KEY idx_etgd_publish (tenant_id, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='理论知识试题生成草稿';
