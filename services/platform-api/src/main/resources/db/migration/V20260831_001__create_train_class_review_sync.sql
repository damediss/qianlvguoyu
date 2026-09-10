-- 班级同步学配置（通道 C 绑定层）：class_id → exam_id
-- UK 对齐 train_class_practice_scope：不含 deleted，删班时物理清理

CREATE TABLE IF NOT EXISTS train_class_review_sync (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  class_id        BIGINT       NOT NULL COMMENT 'train_info_class.id',
  exam_id         BIGINT       NOT NULL COMMENT 'train_exam_list.id',
  enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  sync_start_time DATETIME     NULL COMMENT '班级开练开始，空=不限',
  sync_end_time   DATETIME     NULL COMMENT '班级开练截止，空=不限',
  auto_enroll     TINYINT      NOT NULL DEFAULT 0 COMMENT '预留：保存时是否自动补名单',
  remark          VARCHAR(255) NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_class_review_sync (tenant_id, class_id),
  KEY idx_class_review_sync_exam (tenant_id, exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级同步学配置（通道C绑定层）';
