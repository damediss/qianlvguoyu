-- 竞赛题库只保存题目内容与答案，题目分值由组卷计划明细统一冻结。
-- 保留历史 score 列用于兼容已存在数据，但新建/更新题目不再写入或校验该字段。
ALTER TABLE competition_question
  DROP CHECK chk_competition_question_score,
  MODIFY COLUMN score DECIMAL(12,2) NULL COMMENT '历史兼容字段，实际分值以组卷计划为准';
