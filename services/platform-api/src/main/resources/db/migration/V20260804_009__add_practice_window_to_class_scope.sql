-- 班级刷题可选时间窗：NULL 表示不限制（不过期）
-- 目标列：train_class_practice_scope.practice_start_time / practice_end_time
-- 远端库已手工补列；Druid WallFilter 禁止 Flyway 执行 PREPARE 动态 SQL，此处保留幂等占位。
-- 若全新库缺列，请手工执行：
--   ALTER TABLE train_class_practice_scope
--     ADD COLUMN practice_start_time DATETIME NULL COMMENT '刷题开放开始时间，空=不限开始' AFTER remark,
--     ADD COLUMN practice_end_time DATETIME NULL COMMENT '刷题截止时间，空=不过期' AFTER practice_start_time;
SELECT 1;
