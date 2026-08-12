-- 跟班模拟考关联题库组卷计划
ALTER TABLE train_follow_task
  ADD COLUMN make_exam_plan_id INT NULL COMMENT '来源组卷计划 exam_make_exam_plan.id' AFTER published_at,
  ADD COLUMN source_paper_id INT NULL COMMENT '来源正式卷 exam_paper.id' AFTER make_exam_plan_id;

CREATE INDEX idx_follow_task_make_plan ON train_follow_task (make_exam_plan_id);
