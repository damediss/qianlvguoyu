-- V20260805_001: AI 同步学多选严格判分开关（O7 拍板，2026-08-05 需求方确认）
-- 1) 作答记录增加实得分列（严格判分部分分；空 = 按 is_correct 全对/零分推导）
-- 2) sys_dict 新增开关：ai_review_mock_strict_judge=1（模考多选严格判分：错选 0 分、少选按选项 score 部分给分）

ALTER TABLE ai_review_answer
  ADD COLUMN earned_score DECIMAL(10,1) NULL COMMENT '实得分（严格判分部分分；空=按 is_correct 推导）' AFTER question_score,
  ADD COLUMN mock_paper_id BIGINT NULL COMMENT '模考卷 id（round_type=mock 时，作答幂等键用）' AFTER day_id;

INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20260805011, 1, 'ai_review_mock_strict_judge', 'AI同步学：模考多选严格判分开关（1=错选0分/少选部分分，0=PHP宽松口径）', '1', 'enabled', NOW(), NOW(), 1, 1, 0);
