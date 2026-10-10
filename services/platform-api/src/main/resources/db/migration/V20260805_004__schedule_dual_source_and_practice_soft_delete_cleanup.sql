-- 跟班课表双源治理：记录是否经跟班手工调课、最近一次从开班 Excel 导入时间
-- 并清理练习范围软删行（避免 uk_* 被 deleted=1 占住）
-- 版本选 20260805.004：001–003 已被其他脚本占用

ALTER TABLE train_info_class
  ADD COLUMN schedule_manual_edited TINYINT NOT NULL DEFAULT 0
    COMMENT '1=跟班/课表配置已手工保存，重导开班课表需二次确认' AFTER kcbiao_file,
  ADD COLUMN schedule_imported_at DATETIME NULL
    COMMENT '最近一次从 kcbiao_file 导入 train_schedule 的时间' AFTER schedule_manual_edited;

-- 清理历史软删白名单，释放唯一键
DELETE FROM train_class_practice_knowledge WHERE deleted = 1;
DELETE FROM train_class_practice_course WHERE deleted = 1;
DELETE FROM train_class_practice_scope WHERE deleted = 1;
DELETE FROM train_schedule WHERE deleted = 1;
