-- 为 exam_exam_detail 增加 update_time 列
-- 背景: 原 V20260609_001 建表时未带 update_time,Entity/VO 也没有该字段。
--       本迁移补齐该列,由 MySQL 的 ON UPDATE CURRENT_TIMESTAMP 自动维护。
ALTER TABLE exam_exam_detail
  ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    COMMENT '更新时间,记录最近一次修改时刻' AFTER create_time;

-- 回填: 已存在数据 update_time 沿用 create_time
UPDATE exam_exam_detail
  SET update_time = create_time
  WHERE update_time IS NULL;
