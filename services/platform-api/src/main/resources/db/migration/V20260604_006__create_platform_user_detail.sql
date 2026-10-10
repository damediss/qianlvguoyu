-- 对齐 PHP platform_user_detail（学员详情，开班材料填充）
CREATE TABLE IF NOT EXISTS platform_user_detail (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL COMMENT '平台用户ID',
  realname VARCHAR(100) NULL COMMENT '姓名',
  phone VARCHAR(32) NULL COMMENT '手机号',
  idcard VARCHAR(32) NULL COMMENT '身份证号',
  gender INT NULL COMMENT '0男 1女(PHP约定)',
  nation VARCHAR(50) NULL COMMENT '民族',
  education VARCHAR(100) NULL COMMENT '文化程度',
  political VARCHAR(100) NULL COMMENT '政治面貌',
  address VARCHAR(500) NULL COMMENT '家庭住址',
  idcard_header VARCHAR(500) NULL COMMENT '证件照路径或URL',
  birthday VARCHAR(32) NULL COMMENT '出生日期',
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_platform_user_detail_idcard (idcard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
