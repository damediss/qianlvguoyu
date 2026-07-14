-- 查证批次表 + 明细表 + chazheng_cert 唯一索引

-- 1. 查证批次表
CREATE TABLE IF NOT EXISTS chazheng_cert_batch (
  id BIGINT NOT NULL COMMENT '批次 ID（雪花 ID）',
  total_count INT NOT NULL DEFAULT 0 COMMENT '总用户数',
  completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成数(原子 UPDATE 递增)',
  status VARCHAR(32) NOT NULL DEFAULT 'processing'
    COMMENT 'processing / completed / failed',
  source_filename VARCHAR(255) NULL COMMENT '导入的原始文件名(Excel),手动触发的批次为 NULL',
  trigger_by BIGINT NULL COMMENT '触发人用户 ID',
  triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  error_message TEXT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chazheng_cert_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查证批次';

-- 2. 查证明细表
CREATE TABLE IF NOT EXISTS chazheng_cert_batch_item (
  id BIGINT NOT NULL COMMENT '明细 ID（雪花 ID）',
  batch_id BIGINT NOT NULL COMMENT '批次 ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  user_name VARCHAR(64) NULL COMMENT '用户姓名（冗余，便于排查）',
  status VARCHAR(32) NOT NULL DEFAULT 'pending'
    COMMENT 'pending / processing / success / success_no_cert / failed',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  error_message VARCHAR(500) NULL,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_batch_item_batch (batch_id, status),
  KEY idx_batch_item_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查证明细';

-- 唯一索引由 V20260709_007__add_chazheng_cert_unique_index.sql 维护，此处不再重复创建
