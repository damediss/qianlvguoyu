-- P1（理论知识试题生成-并发与成本优化方案 v0.3 R8）：批次编排独占权
-- 场景：pause→resume 在块在飞时，旧编排器可能读到恢复后的 RUNNING 继续派发，与新编排器形成双编排。
-- run_id 每次 submit/resume/retryFailed 重新生成，processBatch 逐块校验一致才继续派发，旧编排器自行退出。

ALTER TABLE exam_theory_gen_batch
  ADD COLUMN run_id BIGINT NULL COMMENT '编排独占权：每次编排（submit/resume/retry）生成新雪花ID，逐块校验一致才继续派发' AFTER prompt_digest;
