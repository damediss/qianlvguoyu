-- 人社成绩花名册：准考证号落库（考试页人员列表展示）
ALTER TABLE train_people_list
  ADD COLUMN ticket_no VARCHAR(64) NULL COMMENT '准考证号' AFTER id_card;
