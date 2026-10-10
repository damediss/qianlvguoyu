-- 跟班课表展示：学时、授课形式（与开班课表 Excel 列一致）
ALTER TABLE train_schedule
  ADD COLUMN class_hours VARCHAR(32) NULL COMMENT '学时' AFTER course_name,
  ADD COLUMN teach_form VARCHAR(64) NULL COMMENT '授课形式（理论/实操等）' AFTER class_hours;
