-- 知识归属修正·关键词剪枝（docs/05_decisions/知识归属修正_JEV分层改造方案.md v1.3 §7.2 附一/附四，§13 步骤 11）
-- 取号依据：测试库 gjjddb2026 MAX(version)=20261001.111、生产库 gjjddbpro MAX(version)=20261001.110，取最大留间隔
-- 两个剪枝开关挂在 knowledge-fix-jev 配置行上（批次启动时读配置固化，改配置对下一批次生效）；
-- stage2 剪枝作用于 JEV 与 LLM 兜底两条通道，stage1 宽剪仅作用于 JEV 通道（附一硬约束 1）。
-- 通道关闭（status≠enabled）时配置行仍可被读取以取剪枝开关——剪枝对 LLM 路径独立生效（附一 KP-11）。

-- 1) 模型配置表：剪枝开关（默认全关 = 既有路径，回退即原路径）
ALTER TABLE exam_faq_model_config
  ADD COLUMN jev_prune_stage2 TINYINT NOT NULL DEFAULT 0 COMMENT 'stage2 剪枝开关：兄弟内关键词 top-8（1=开）。作用于 JEV 与 LLM 兜底两通道，0=原路径',
  ADD COLUMN jev_prune_stage1 TINYINT NOT NULL DEFAULT 0 COMMENT 'stage1 宽剪开关：L2 证据聚合 top-5（1=开）。仅 JEV 通道，LLM 全流程 stage1 恒全量（附一 KP-18）';

-- 2) 审计表：剪枝归因（附一 KP-22，供 A4 错 L2 归因回查——缺此无法区分"选错"与"被剪掉"）
ALTER TABLE exam_knowledge_fix_llm_log
  ADD COLUMN pruned_candidates INT NULL COMMENT '剪枝后候选数（stage1=L2 短名单、stage2=兄弟集；NULL=该次调用未启用剪枝，如 LLM stage1 恒全量）',
  ADD COLUMN prune_weak_fallback TINYINT NULL COMMENT '弱信号回退：1=题干与候选标题零词面重合回退全量，0=正常剪枝，NULL=未启用剪枝',
  ADD COLUMN prune_reserved_old TINYINT NULL COMMENT 'keep 保位：1=老节点/老 L2 被强制保留槽位，0=已在名单内或无老节点，NULL=未启用剪枝';
