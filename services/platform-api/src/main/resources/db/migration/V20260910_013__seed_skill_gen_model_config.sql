-- 操作技能试题生成：GLM-5.3-Flash 模型配置种子（方案 v1.1 迁移 4 / C6）
-- api_key 为占位符，部署方手工回填真实 key；严禁真实 key 出现在仓库任何文件
-- tenant_id=10159 与该表既有种子一致（listEnabled 按 tenant 精确匹配）
-- model_id 用官方编码全小写 glm-5.3-flash（理论链路 P0 教训：大小写不符为隐性风险）
-- thinking=enabled + reasoning_effort=high（需求方指定高档位）；max_tokens=16384（三件套输出体量大）
-- 版本号依据（2026-09-10 双库查询）：gjjddb2026(测试) MAX=20260909.004；gjjddbpro(生产) MAX=20260909.003，
-- 本组迁移取 20260910.010~013（踩坑 #7）。

INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, thinking, reasoning_effort, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  202609100001, 10159,
  'skill-gen-glm',
  'GLM-5.3-Flash',
  'https://open.bigmodel.cn/api/paas/v4',
  'PENDING_USER_INPUT',
  'openai_compatible',
  'glm-5.3-flash',
  0.70, 16384,
  'enabled', 'high', NULL,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  base_url = VALUES(base_url),
  model_id = VALUES(model_id),
  max_tokens = VALUES(max_tokens),
  thinking = VALUES(thinking),
  reasoning_effort = VALUES(reasoning_effort),
  updated_at = NOW();
