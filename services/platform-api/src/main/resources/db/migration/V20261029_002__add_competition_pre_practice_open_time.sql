ALTER TABLE competition_pre_practice_task
  ADD COLUMN effective_time_enabled TINYINT NOT NULL DEFAULT 0
    COMMENT '0=推送后立即开放且无截止时间，1=按开放时间窗口限制'
    AFTER title,
  ADD COLUMN effective_start_at DATETIME NULL
    COMMENT '开放开始时间；仅启用开放时间限制时有效'
    AFTER effective_time_enabled,
  ADD COLUMN effective_end_at DATETIME NULL
    COMMENT '开放截止时间；仅启用开放时间限制时有效'
    AFTER effective_start_at;
