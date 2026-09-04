-- 理论知识试题生成：GLM-5.3-Flash 模型配置种子（方案 v1.2 迁移 4 / C3）
-- api_key 为占位符，部署方手工回填真实 key；严禁真实 key 出现在仓库任何文件
-- tenant_id=10159 与该表既有种子一致（listEnabled 按 tenant 精确匹配，不落 tenant 0）
-- 版本号依据（2026-09-04 双库查询）：gjjddbpro(生产) MAX=20260831.001；
-- gjjddb2026(测试) 已被 train_certificate 工作流占用 20260904.001/.002，
-- 本组迁移取 20260904.010~013（踩坑 #7）。原 20260904_002 编号从未在任何库应用。

INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, thinking, reasoning_effort, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  202609040001, 10159,
  'theory-gen-glm',
  'GLM-5.3-Flash',
  'https://open.bigmodel.cn/api/paas/v4',
  'PENDING_USER_INPUT',
  'openai_compatible',
  'GLM-5.3-Flash',
  0.70, 8192,
  'enabled', 'low', NULL,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  base_url = VALUES(base_url),
  model_id = VALUES(model_id),
  thinking = VALUES(thinking),
  reasoning_effort = VALUES(reasoning_effort),
  updated_at = NOW();
