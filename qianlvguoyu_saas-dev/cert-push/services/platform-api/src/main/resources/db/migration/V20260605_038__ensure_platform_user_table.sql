-- 对齐 PHP platform_user（共享库可能已存在）
CREATE TABLE IF NOT EXISTS platform_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  realname VARCHAR(100) NULL COMMENT '姓名',
  idcard VARCHAR(20) NULL COMMENT '身份证号',
  phone VARCHAR(20) NULL COMMENT '手机号',
  password VARCHAR(64) NULL COMMENT '密码',
  stat VARCHAR(16) NULL COMMENT '密码盐',
  addtime BIGINT NULL COMMENT '创建时间戳',
  logintime BIGINT NULL COMMENT '登录时间戳',
  org_id BIGINT NULL COMMENT '机构ID',
  company_id BIGINT NULL COMMENT '企业ID',
  wxopenid VARCHAR(128) NULL COMMENT '微信openid',
  expert_id BIGINT NULL COMMENT '专家ID',
  task INT NULL DEFAULT 0 COMMENT '任务标记',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_platform_user_org (org_id),
  INDEX idx_platform_user_company (company_id),
  INDEX idx_platform_user_idcard (idcard),
  INDEX idx_platform_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
