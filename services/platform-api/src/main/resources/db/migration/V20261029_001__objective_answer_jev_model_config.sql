-- 客观题答案 AI 检查 v1.5：JEV 连接配置集中入库（方案 §12.6）
-- 取号：2026-10-29 实施当天实测测试库 gjjddb2026 MAX=20261028.005、生产库 gjjddbpro MAX=20261027.006，
--       共用目录取 20261029.001。原 V20261028_005 已应用/可能已应用，本条为后续迁移，不改历史迁移。
-- 安全口径：api_key 固定为说明性占位文案，真实密钥只走 OBJECTIVE_ANSWER_CHECK_JEV_API_KEY 环境变量。

-- 1) 共享模型配置表：区分适用范围，避免本行污染 FAQ/理论/技能生成模型下拉
ALTER TABLE exam_faq_model_config
  ADD COLUMN feature_scope VARCHAR(32) NOT NULL DEFAULT 'common'
    COMMENT '配置适用范围：common/faq-conversion/theory-gen/skill-gen/objective-answer-check/knowledge-fix' AFTER api_format,
  ADD KEY idx_efmc_scope_status (tenant_id, feature_scope, status);

-- 2) 答案检查批次表：固化 v1.5 JEV 运行时快照（非密钥），重启续跑仍用创建时配置
ALTER TABLE exam_objective_answer_check_batch
  ADD COLUMN jev_base_url VARCHAR(512) NULL COMMENT '批次创建时从模型配置固化的JEV Base URL（非密钥）',
  ADD COLUMN jev_concurrency INT NULL COMMENT '批次创建时从模型配置固化的JEV派发并发',
  ADD COLUMN jev_read_timeout_seconds INT NULL COMMENT '批次创建时从模型配置固化的JEV读超时';

-- 3) 客观题答案检查专用 JEV 配置行：默认禁用，部署环境密钥后由管理员启用
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name, base_url, api_key, api_format, feature_scope,
  model_id, temperature, max_tokens, thinking, reasoning_effort, extra_config, status,
  jev_theta1, jev_theta2, jev_concurrency, jev_read_timeout_seconds,
  jev_prune_stage2, jev_prune_stage1,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202610290101, 10159,
  'OBJECTIVE_ANSWER_CHECK_JEV_MODEL', '客观题答案检查 JEV',
  'https://tokendance.space/gateway', '模型API Key走配置文件', 'custom', 'objective-answer-check',
  'ateve-jev-v1', 0.70, 2000, NULL, NULL, NULL, 'disabled',
  NULL, NULL, 5, 30,
  0, 0,
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM exam_faq_model_config c
  WHERE c.tenant_id = 10159
    AND c.model_code = 'OBJECTIVE_ANSWER_CHECK_JEV_MODEL'
    AND c.deleted = 0
);
