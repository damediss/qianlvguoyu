-- 班级刷题可选时间窗：NULL 表示不限制（不过期）
ALTER TABLE train_class_practice_scope
  ADD COLUMN practice_start_time DATETIME NULL COMMENT '刷题开放开始时间，空=不限开始' AFTER remark,
  ADD COLUMN practice_end_time DATETIME NULL COMMENT '刷题截止时间，空=不过期' AFTER practice_start_time;
