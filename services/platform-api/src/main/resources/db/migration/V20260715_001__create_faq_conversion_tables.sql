-- 试题 FAQ 转换相关 4 张新表
-- 1. FAQ 转换记录表
CREATE TABLE IF NOT EXISTS exam_faq_conversion (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',

  -- 溯源关联
  batch_id BIGINT NULL COMMENT '所属批次 exam_faq_conversion_batch.id',
  exam_id INT NULL COMMENT '来源试题 exam_exam_detail.id',
  work_type_id INT NOT NULL COMMENT '工种 ID exam_work_type.id',
  course_id INT NULL COMMENT '科目 ID exam_course.id',
  knowledge_id INT NULL COMMENT '知识点 ID exam_knowledge_structure.id',

  -- 原始试题快照
  source_type VARCHAR(50) NULL COMMENT '题型: single/multi/judge',
  source_difficulty INT NULL COMMENT '难度',
  source_content TEXT NULL COMMENT '原始题干',
  source_answer TEXT NULL COMMENT '原始答案 JSON',

  -- 结构化 FAQ 字段
  standard_question TEXT NOT NULL COMMENT '标准问',
  answers JSON NOT NULL COMMENT '答案数组',
  similar_questions JSON NULL COMMENT '相似问数组（LLM 生成）',
  negative_questions JSON NULL COMMENT '反例数组（LLM 生成）',
  tag_name VARCHAR(500) NULL COMMENT '标签路径：题型|知识点末三级|难度',

  -- 转换元数据
  conversion_status VARCHAR(32) NOT NULL DEFAULT 'pending'
    COMMENT 'pending=待转换 / processing=转换中 / success=成功 / failed=失败 / skipped=跳过',
  model_code VARCHAR(64) NULL COMMENT '使用的 LLM 模型编码',
  error_message VARCHAR(1024) NULL COMMENT '转换失败原因',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',

  -- 导入状态
  weknora_imported TINYINT NOT NULL DEFAULT 0 COMMENT '是否已导入 WeKnora: 0=否 1=是',
  weknora_imported_at DATETIME NULL COMMENT '导入 WeKnora 时间',

  -- 审计字段
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删 1=已删',

  PRIMARY KEY (id),
  UNIQUE KEY uk_exam_faq_conversion_exam (tenant_id, exam_id, deleted),
  KEY idx_efc_work_type_status (tenant_id, work_type_id, conversion_status),
  KEY idx_efc_status (tenant_id, conversion_status),
  KEY idx_efc_knowledge (tenant_id, knowledge_id),
  KEY idx_efc_batch (tenant_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='试题 FAQ 结构化转换记录表';

-- 2. 转换批次表
CREATE TABLE IF NOT EXISTS exam_faq_conversion_batch (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',

  -- 转换范围
  work_type_id INT NOT NULL COMMENT '工种 ID',
  work_type_name VARCHAR(255) NULL COMMENT '工种名称（快照）',
  course_ids JSON NULL COMMENT '选中的科目 ID 列表（空=全部科目）',

  -- 模型配置（快照）
  model_code VARCHAR(64) NULL COMMENT '本次使用的模型编码',
  model_name VARCHAR(128) NULL COMMENT '模型名称快照',

  -- 统计
  total_exams INT NOT NULL DEFAULT 0 COMMENT '参与转换的试题总数',
  success_count INT NOT NULL DEFAULT 0 COMMENT '转换成功数',
  failed_count INT NOT NULL DEFAULT 0 COMMENT '转换失败数',
  skipped_count INT NOT NULL DEFAULT 0 COMMENT '跳过数',
  pending_count INT NOT NULL DEFAULT 0 COMMENT '待转换数',

  -- 批次状态
  status VARCHAR(32) NOT NULL DEFAULT 'running'
    COMMENT 'running=执行中 / paused=暂停 / completed=完成 / canceled=取消',
  started_at DATETIME NULL COMMENT '开始时间',
  completed_at DATETIME NULL COMMENT '完成时间',
  error_message VARCHAR(1024) NULL COMMENT '错误信息',

  -- 并发控制
  concurrency_limit INT NOT NULL DEFAULT 5 COMMENT '并发批次数',

  -- 审计字段
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删 1=已删',

  PRIMARY KEY (id),
  KEY idx_efcb_work_type (tenant_id, work_type_id),
  KEY idx_efcb_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='试题 FAQ 转换批次表';

-- 3. LLM 模型连接参数配置表
CREATE TABLE IF NOT EXISTS exam_faq_model_config (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  model_code VARCHAR(64) NOT NULL COMMENT '模型编码（唯一标识，如 deepseek-v3）',
  model_name VARCHAR(128) NOT NULL COMMENT '模型显示名称',
  base_url VARCHAR(512) NOT NULL COMMENT 'API Base URL',
  api_key VARCHAR(512) NOT NULL COMMENT 'API 密钥',
  api_format VARCHAR(32) NOT NULL DEFAULT 'openai_compatible'
    COMMENT 'API 格式: openai_compatible / anthropic / custom',
  model_id VARCHAR(128) NOT NULL COMMENT '模型 ID（请求 body 中的 model 字段值）',
  temperature DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '采样温度',
  max_tokens INT NOT NULL DEFAULT 2000 COMMENT '最大输出 token',
  extra_config JSON NULL COMMENT '扩展配置（可选，存储自定义 headers 等）',
  status VARCHAR(32) NOT NULL DEFAULT 'enabled' COMMENT 'enabled / disabled',

  -- 审计字段
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删 1=已删',

  PRIMARY KEY (id),
  UNIQUE KEY uk_model_config_code (tenant_id, model_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='FAQ 转换 LLM 模型连接参数配置表';

-- 4. WeKnora 集成配置表
CREATE TABLE IF NOT EXISTS exam_faq_weknora_config (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',

  -- 签名与事件配置
  webhook_secret VARCHAR(256) NULL COMMENT '签名密钥（共享给 WeKnora，用于 sha256 校验）',
  enabled_events JSON NULL COMMENT '启用的事件列表',

  -- FAQ 导出目标
  knowledge_base_id VARCHAR(128) NULL COMMENT 'WeKnora 知识库 ID',
  faq_set_id VARCHAR(128) NULL COMMENT 'FAQ 数据集 ID',

  -- 功能开关
  webhook_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Webhook 接收开关: 0=关 1=开',
  auto_export TINYINT NOT NULL DEFAULT 0 COMMENT '转换成功后自动导出到 WeKnora: 0=否 1=是',

  -- 审计字段
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删 1=已删',

  PRIMARY KEY (id),
  UNIQUE KEY uk_weknora_config_tenant (tenant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='WeKnora 集成配置表（回调签名校验 + FAQ 导出目标）';
