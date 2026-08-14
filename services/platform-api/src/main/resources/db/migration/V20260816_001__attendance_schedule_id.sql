-- O1 KR3：考勤挂课次 schedule_id
-- 远端库已预执行加列；Druid WallFilter 禁止 PREPARE，裸 ADD 遇已有列会失败，故本脚本仅占位。
-- 全新库请手工执行：
--   ALTER TABLE train_attendance
--     ADD COLUMN schedule_id BIGINT NULL COMMENT '课次 train_schedule.id；空=按日考勤' AFTER attendance_date;
--   CREATE INDEX idx_train_attendance_schedule ON train_attendance (class_id, attendance_date, schedule_id, deleted);

SELECT 1;
