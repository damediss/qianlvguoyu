-- 1. 试题 FAQ 知识管理菜单（挂在「提升/考试管理」下，sort=4）
-- 先尝试更新已存在 ID（避免重复插入），再 INSERT
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name,
  path, component, permission_code,
  icon, sort_order, visible, expanded, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  -- 空库幂等化（踩坑 #34 修复）：链上 sys_menu.expanded 为 NOT NULL 无默认（PHP 老表可空），
  -- 原值 NULL 在空库必报 cannot be null；改 0（不展开），语义等价。存量库本版本已应用不会重跑。
  6600, 1, 2064170197574156289, 'page', 'exam_faq_conversion', '试题FAQ知识管理',
  '/question-bank/faq-conversion', 'exam/FaqConversionPage', 'exam:faq:view',
  'RobotOutlined', 4, 1, 0, 'enabled',
  NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

-- 2. 默认 LLM 模型配置（占位符 API Key，需用户在页面填入真实密钥）
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  1,
  0,
  'longcat-2.0',
  'LongCat-2.0',
  'https://api.longcat.chat/openai/v1',
  'PENDING_USER_INPUT',
  'openai_compatible',
  'LongCat-2.0',
  0.70,
  2000,
  NULL,
  'enabled',
  NOW(), NOW(),
  NULL, NULL, 0
)
ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  base_url = VALUES(base_url),
  updated_at = NOW();
