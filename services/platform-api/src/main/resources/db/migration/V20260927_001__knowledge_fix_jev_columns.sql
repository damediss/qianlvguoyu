-- 知识归属修正接入 bocha-jev-v1 分层改造（docs/05_decisions/知识归属修正_JEV分层改造方案.md v0.8 §8.5）
-- 取号依据：测试库 gjjddb2026 MAX(version)=20260925.010、生产库 gjjddbpro MAX(version)=20260924.001，取最大留间隔

-- 1) 批次表：Jev/LLM 构成统计
ALTER TABLE exam_knowledge_fix_batch
  ADD COLUMN jev_classified INT NOT NULL DEFAULT 0 COMMENT 'Jev 通道分类行数（apply/keep/flag/jev-l2-direct）',
  ADD COLUMN llm_fallback INT NOT NULL DEFAULT 0 COMMENT '走 LLM 全流程兜底行数（含通道关闭/未过双门/Jev 失败）',
  ADD COLUMN jev_avg_confidence DECIMAL(5,4) NULL COMMENT 'Jev 行平均置信度（信息项，不卡门禁）';

-- 2) 模型配置表：Jev 通道参数（批次启动时读配置固化，改配置对下一批次生效）
ALTER TABLE exam_faq_model_config
  ADD COLUMN jev_theta1 DECIMAL(4,3) NULL COMMENT 'Jev 阶段1（L2）置信门限，NULL=默认0.5，硬底线≥0.3由应用校验',
  ADD COLUMN jev_theta2 DECIMAL(4,3) NULL COMMENT 'Jev 阶段2（L3）置信门限，NULL=默认0.5，<0.3 应用校验拒绝',
  ADD COLUMN jev_concurrency INT NULL COMMENT 'Jev 独立并发上限，NULL=默认5，不与 LLM 并发池混用',
  ADD COLUMN jev_read_timeout_seconds INT NULL COMMENT 'Jev 读取超时秒数，NULL=默认30（实测 e2e 1~5.6s@255 选项）';

-- 3) 审计表：Jev 响应元数据（响应 model 记入现有 model_id 列，无需新列）
ALTER TABLE exam_knowledge_fix_llm_log
  ADD COLUMN confidence_definition VARCHAR(128) NULL COMMENT 'Jev 响应 metadata.confidence_definition（口径随路由漂移，须落库）',
  ADD COLUMN top1_prob DECIMAL(8,6) NULL COMMENT 'Jev 概率分布 top1',
  ADD COLUMN top2_prob DECIMAL(8,6) NULL COMMENT 'Jev 概率分布 top2';
