-- train_sign_up_selftask：默认模板异步导出任务（对齐 PHP）

CREATE TABLE IF NOT EXISTS train_sign_up_selftask (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sign_up_id BIGINT NOT NULL COMMENT '报名计划ID',
  ids_in TEXT NULL COMMENT '待导出人员ID JSON',
  ids_out TEXT NULL COMMENT '已完成人员ID JSON',
  addtime BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间戳(秒)',
  endtime BIGINT NOT NULL DEFAULT 0 COMMENT '结束时间戳(秒)，0=进行中',
  error_msg VARCHAR(500) NULL COMMENT '失败信息',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  INDEX idx_sign_up_selftask_sign_up (sign_up_id),
  INDEX idx_sign_up_selftask_endtime (endtime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
