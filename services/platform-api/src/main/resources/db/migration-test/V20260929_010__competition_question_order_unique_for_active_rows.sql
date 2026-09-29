-- 竞赛试题序号只对未删除题目保持唯一，软删除记录不再占用序号。
ALTER TABLE competition_question
  DROP INDEX uk_competition_question_scope_order,
  ADD UNIQUE KEY uk_competition_question_scope_order (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, question_type,
    (CASE WHEN deleted = 0 THEN sort_order ELSE NULL END)
  );
