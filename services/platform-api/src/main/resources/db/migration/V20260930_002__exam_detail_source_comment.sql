-- exam_exam_detail.source 列注释与实际取值对齐：
-- 0=内部(手工/导入内部), 1=外部(导入外部), 2=AI生成, 3=赛前复习(组卷抽题排除)
ALTER TABLE exam_exam_detail
  MODIFY COLUMN source INT NULL DEFAULT 0 COMMENT '来源: 0=内部, 1=外部, 2=AI生成, 3=赛前复习';
