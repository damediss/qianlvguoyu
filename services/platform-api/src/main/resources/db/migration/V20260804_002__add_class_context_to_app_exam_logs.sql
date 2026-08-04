-- App 刷题日志补班级上下文，支撑班级刷题学情

ALTER TABLE exam_user_answer_log
  ADD COLUMN class_id BIGINT NULL COMMENT '刷题班级 train_info_class.id' AFTER tenant_id,
  ADD COLUMN people_id BIGINT NULL COMMENT '花名册 train_people_list.id' AFTER class_id,
  ADD COLUMN course_id INT NULL COMMENT '科目 exam_course.id' AFTER people_id,
  ADD COLUMN biz_date DATE NULL COMMENT '练习自然日' AFTER course_id;

ALTER TABLE exam_mock_paper
  ADD COLUMN class_id BIGINT NULL COMMENT '刷题班级' AFTER tenant_id,
  ADD COLUMN people_id BIGINT NULL COMMENT '花名册学员' AFTER class_id,
  ADD COLUMN course_id INT NULL COMMENT '主科目' AFTER people_id;

CREATE INDEX idx_answer_log_class_user_time
  ON exam_user_answer_log (tenant_id, class_id, user_id, create_time);

CREATE INDEX idx_answer_log_class_knowledge
  ON exam_user_answer_log (tenant_id, class_id, knowledge_id, is_right);

CREATE INDEX idx_mock_paper_class_user
  ON exam_mock_paper (tenant_id, class_id, user_id);
