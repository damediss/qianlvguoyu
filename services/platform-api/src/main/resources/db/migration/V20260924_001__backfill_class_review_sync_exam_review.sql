-- 班级同步学开通后，考试计划须有 train_exam_review 行，App 才会归入「复习任务」并显示「同步练习」。
-- 历史数据：名单/灰度已写、复习配置缺失 → 本脚本补齐。

INSERT INTO train_exam_review (exam_id, paper_id, num, user_id, tenant_id, status, sort_number)
SELECT s.exam_id,
       1,
       0,
       0,
       COALESCE(NULLIF(s.tenant_id, 0), 10159),
       1,
       1
FROM train_class_review_sync s
WHERE s.deleted = 0
  AND s.enabled = 1
  AND s.exam_id IS NOT NULL
  AND s.exam_id > 0
  AND NOT EXISTS (
    SELECT 1 FROM train_exam_review r WHERE r.exam_id = s.exam_id
  );
