-- 模型配置表新增 reasoning_effort 字段（DeepSeek 推理强度：low/high/max，默认 high）
-- 仅 thinking.type=enabled 时生效；仅 DeepSeek 模型发送（LongCat 等其它模型不支持该字段，不传）
-- 版本号依据：DB flyway_schema_history 当前 MAX(version)=20260811.005（2026-08-11 查询）
ALTER TABLE exam_faq_model_config
  ADD COLUMN reasoning_effort VARCHAR(8) NOT NULL DEFAULT 'high'
  COMMENT 'DeepSeek 推理强度：low/high/max，仅 thinking=enabled 时随 thinking 节点发送';
