-- P2（理论知识试题生成-并发与成本优化方案 v0.3）：token 用量细分落库
-- 缓存命中观测口径（智谱官方）：usage.prompt_tokens_details.cached_tokens；
-- DeepSeek 口径 usage.prompt_cache_hit_tokens 由代码层归一到 cached_tokens。
-- 既有 token_usage（total_tokens 累加）保持兼容。

ALTER TABLE exam_theory_gen_batch
  ADD COLUMN prompt_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'prompt_tokens 累计' AFTER token_usage,
  ADD COLUMN completion_tokens BIGINT NOT NULL DEFAULT 0 COMMENT 'completion_tokens 累计' AFTER prompt_tokens,
  ADD COLUMN cached_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '缓存命中 tokens（GLM cached_tokens / DeepSeek prompt_cache_hit_tokens）' AFTER completion_tokens;
