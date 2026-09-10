-- 操作技能试题生成管理：细目表批次 + 节点 + 生成批次 + 试题草稿（4 张新表）
-- 方案：docs/04_requirements/操作技能试题生成管理方案.md v1.1（T1 迁移 1）
-- 版本号依据（2026-09-10 双库查询）：gjjddb2026(测试) MAX=20260909.004；gjjddbpro(生产) MAX=20260909.003，
-- 故本组迁移取 20260910.010~013，避开两库已用号与并行工作流（踩坑 #7）。
-- 说明：一期不写 exam_knowledge_structure / exam_course（方案 C2/S1），本组表不含 course_id / knowledge_id。

-- 1. 细目表批次表
CREATE TABLE IF NOT EXISTS exam_skill_blueprint (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  work_type_id INT NOT NULL COMMENT '工种 ID exam_work_type.id',
  level TINYINT NOT NULL COMMENT '等级 1=一级/高级技师 … 5=五级/初级工（数字=国标级别数，与 task_examshicao.level 同口径）',
  title VARCHAR(255) NOT NULL COMMENT '细目表标题，如 矿山生产集控员（二级/技师）操作技能考评要素细目表',
  source_file_name VARCHAR(255) NULL COMMENT '上传文件名',
  source_file_id BIGINT NULL COMMENT '源细目表文件 ID（sys_file.id），导入时留存，供事后下载核对',
  point_count INT NOT NULL DEFAULT 0 COMMENT '考评点数量',
  xyz_ratio VARCHAR(20) NULL COMMENT '考评点重要程度比例，如 18:5:0（仅统计展示，不做硬校验）',
  weight_check TINYINT NOT NULL DEFAULT 0 COMMENT '一级权重合计校验 0未通过 1通过',
  check_report JSON NULL COMMENT '导入校验报告',
  status VARCHAR(32) NOT NULL DEFAULT 'imported' COMMENT 'imported已导入 generating生成中 done已完成',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_esb_tenant (tenant_id),
  KEY idx_esb_work_level (tenant_id, work_type_id, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='操作技能考评要素细目表批次';

-- 2. 细目表节点表（一/二级考评范围 + 考评点）
CREATE TABLE IF NOT EXISTS exam_skill_blueprint_node (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  blueprint_id BIGINT NOT NULL COMMENT '批次 exam_skill_blueprint.id',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点 id，0=根',
  node_level TINYINT NOT NULL COMMENT '1一级 2二级 3考评点',
  code VARCHAR(20) NOT NULL COMMENT '本级代码 A/B/001',
  full_code VARCHAR(50) NOT NULL COMMENT '全路径代码，如 A-A-001（考评点，入库与导出按父链现拼）',
  name VARCHAR(500) NOT NULL COMMENT '名称',
  weight DECIMAL(5,2) NULL COMMENT '考评比重（百分比数值 0-100，一/二级）',
  select_mode VARCHAR(20) NULL COMMENT '选考方式 必考/任选一项/任选两项（一/二级）',
  skill_requirement TEXT NULL COMMENT '技能要求编号条目原文（仅二级）',
  importance CHAR(1) NULL COMMENT '重要程度 X/Y/Z（仅考评点）',
  question_quota TINYINT NULL COMMENT '细目表"试题量"列原值（仅存档展示；生成固定按每点 5 题）',
  xyz_stat VARCHAR(20) NULL COMMENT '累计重要程度比例，如 6:0:0（一/二级）',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_esbn_blueprint (tenant_id, blueprint_id),
  KEY idx_esbn_parent (tenant_id, parent_id),
  KEY idx_esbn_full_code (tenant_id, blueprint_id, full_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='操作技能细目表节点';

-- 3. 生成批次表
CREATE TABLE IF NOT EXISTS exam_skill_gen_batch (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  blueprint_id BIGINT NOT NULL COMMENT '细目表批次 exam_skill_blueprint.id',
  work_type_id INT NULL COMMENT '工种 exam_work_type.id（冗余自蓝图）',
  level TINYINT NULL COMMENT '等级 1~5（冗余自蓝图）',
  model_config_id BIGINT NOT NULL COMMENT '模型配置 exam_faq_model_config.id',
  model_id VARCHAR(128) NOT NULL COMMENT '模型 ID 快照',
  model_code VARCHAR(64) NULL COMMENT '模型编码快照',
  thinking VARCHAR(16) NULL COMMENT '思考模式快照 enabled/disabled',
  reasoning_effort VARCHAR(8) NULL COMMENT '推理强度快照 low/high/max',
  quota_config JSON NOT NULL COMMENT '题量配置（一期固定 {"perPoint":5}）',
  status VARCHAR(32) NOT NULL DEFAULT 'pending'
    COMMENT 'pending/running/paused/done/failed',
  total_points INT NOT NULL DEFAULT 0 COMMENT '考评点总数',
  done_points INT NOT NULL DEFAULT 0 COMMENT '已完成考评点数',
  failed_points INT NOT NULL DEFAULT 0 COMMENT '失败考评点数',
  total_questions INT NOT NULL DEFAULT 0 COMMENT '已生成试题数',
  run_id BIGINT NULL COMMENT '编排独占权：每次编排生成新值，逐块校验一致才继续派发',
  token_usage BIGINT NOT NULL DEFAULT 0 COMMENT 'token 用量累计',
  prompt_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'prompt token 累计',
  completion_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'completion token 累计',
  cached_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '缓存命中 token 累计',
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
  KEY idx_esgb_tenant (tenant_id),
  KEY idx_esgb_blueprint (tenant_id, blueprint_id),
  KEY idx_esgb_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='操作技能试题生成批次';

-- 4. 试题草稿表
CREATE TABLE IF NOT EXISTS exam_skill_gen_draft (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  batch_id BIGINT NOT NULL COMMENT '生成批次 exam_skill_gen_batch.id',
  blueprint_id BIGINT NOT NULL COMMENT '细目表批次 exam_skill_blueprint.id',
  node_id BIGINT NOT NULL COMMENT '考评点 exam_skill_blueprint_node.id',
  question_name VARCHAR(300) NOT NULL COMMENT '试题名称',
  score DECIMAL(5,1) NOT NULL DEFAULT 100 COMMENT '本题分值（固定 100 分制）',
  weight DECIMAL(5,2) NULL COMMENT '权重（%=所属二级考评范围权重）',
  duration_minutes INT NULL COMMENT '考核时间（分钟）',
  exam_form VARCHAR(20) NULL COMMENT '考核形式 实操/模拟操作/口试等',
  site_preparation TEXT NULL COMMENT '考场准备（含设备设施表 JSON 子结构）',
  examinee_prepare TEXT NULL COMMENT '考生准备',
  requirements TEXT NULL COMMENT '具体考核要求',
  negatives TEXT NULL COMMENT '否定项说明',
  scoring_items JSON NULL COMMENT '评分标准 [{item,score,scoreDesc,maxDeduct,step}]',
  difficulty TINYINT NOT NULL DEFAULT 3 COMMENT '难度 1~5（一期不让 LLM 填，默认 3，页面可改）',
  consistency TINYINT NOT NULL DEFAULT 5 COMMENT '题目-目标一致性 1-5，默认 5',
  ai_model VARCHAR(100) NULL COMMENT '生成模型',
  ai_thinking VARCHAR(16) NULL COMMENT '生成时思考模式',
  gen_duration_ms INT NULL COMMENT '生成耗时毫秒',
  review_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/approved/rejected',
  review_by BIGINT NULL,
  review_at DATETIME NULL,
  publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未入库 1已入库',
  published_task_id BIGINT NULL COMMENT 'task_examshicao.id',
  published_detail_id BIGINT NULL COMMENT 'task_examshicao_detail.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_esgd_batch (tenant_id, batch_id),
  KEY idx_esgd_node (tenant_id, node_id),
  KEY idx_esgd_review (tenant_id, batch_id, review_status),
  KEY idx_esgd_publish (tenant_id, publish_status),
  -- "全部批次"模式按 created_at 倒序跨批次翻页（建表即带，理论链路 V20260909_002 返工教训）
  KEY idx_esgd_tenant_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='操作技能试题生成草稿';
