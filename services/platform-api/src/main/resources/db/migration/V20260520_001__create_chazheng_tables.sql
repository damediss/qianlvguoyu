CREATE TABLE IF NOT EXISTS `chazheng_user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `enterprise_id` INT DEFAULT NULL COMMENT '企业ID',
  `user_name` VARCHAR(10) DEFAULT NULL COMMENT '姓名',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `gender` INT DEFAULT NULL COMMENT '0男 1女',
  `datas` JSON DEFAULT NULL COMMENT '花名册 JSON',
  `is_cert` INT DEFAULT 0 COMMENT '是否有证书 0/1',
  `is_checked` INT DEFAULT 0 COMMENT '是否已查证 0/1',
  `ymd` VARCHAR(20) DEFAULT NULL COMMENT '批次时间戳',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` INT NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  INDEX `idx_ymd` (`ymd`),
  INDEX `idx_id_card` (`id_card`),
  INDEX `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查证用户';

CREATE TABLE IF NOT EXISTS `chazheng_cert` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL COMMENT '用户ID',
  `cert_no` VARCHAR(100) DEFAULT NULL COMMENT '证书编号',
  `cert_name` VARCHAR(255) DEFAULT NULL COMMENT '证书名称',
  `cert_date` VARCHAR(20) DEFAULT NULL COMMENT '发证日期',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查证证书';