-- 人社成绩花名册：理论 / 工作业绩落库（技能→real_score，综合→exam_score）
ALTER TABLE train_people_list
  ADD COLUMN theory_score DECIMAL(10, 2) NULL COMMENT '理论成绩' AFTER exam_score,
  ADD COLUMN performance_score DECIMAL(10, 2) NULL COMMENT '工作业绩' AFTER real_score;
