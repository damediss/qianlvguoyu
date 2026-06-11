-- 对齐 PHP platform_user_token（App istate 登录令牌）
CREATE TABLE IF NOT EXISTS platform_user_token (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT 'platform_user.id',
  token VARCHAR(128) NOT NULL COMMENT 'App token (istate)',
  addtime BIGINT NULL COMMENT '创建时间戳',
  UNIQUE KEY uk_platform_user_token (token),
  INDEX idx_platform_user_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
