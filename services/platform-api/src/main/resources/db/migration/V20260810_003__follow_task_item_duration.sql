-- 跟班作业：开答时间与答题时长（整卷）
ALTER TABLE train_follow_task_item
  ADD COLUMN started_at DATETIME NULL COMMENT '首次打开试卷时间' AFTER comment_text,
  ADD COLUMN duration_seconds INT NULL COMMENT '答题时长秒（submitted_at - started_at）' AFTER started_at;
