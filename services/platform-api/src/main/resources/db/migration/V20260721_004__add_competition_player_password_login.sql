-- R2 Player password login. The initial password is derived only after SMS-backed activation.
ALTER TABLE competition_player_identity
  ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '选手登录密码哈希',
  ADD COLUMN password_initialized_at DATETIME NULL COMMENT '初始密码生成时间';
