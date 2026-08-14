-- V20260813_005: 课次影像配额 —— 每节课照片5张 + 视频3段×15秒
ALTER TABLE train_follow_evidence
  ADD COLUMN duration_seconds INT NULL COMMENT '视频时长（秒），照片为空' AFTER media_type;
