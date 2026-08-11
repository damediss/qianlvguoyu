-- V20260810_001: 课表调课申请表（App 老师申请 → 管理端审批）
-- 《App课表与调课审批实现方案》S2

CREATE TABLE IF NOT EXISTS train_schedule_change_request (
  id                          BIGINT       NOT NULL COMMENT '雪花 ID',
  class_id                    BIGINT       NOT NULL COMMENT 'train_info_class.id',
  schedule_id                 BIGINT       NULL COMMENT 'train_schedule.id；加课可空',
  change_type                 VARCHAR(32)  NOT NULL COMMENT 'reschedule/replace_teacher/cancel/add',
  before_snapshot             TEXT         NULL COMMENT '原课次 JSON',
  after_payload               TEXT         NULL COMMENT '目标字段 JSON',
  reason                      VARCHAR(500) NULL COMMENT '申请原因',
  status                      VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  applicant_platform_user_id  BIGINT       NOT NULL COMMENT '申请人 platform_user.id',
  applicant_role_code         VARCHAR(32)  NULL COMMENT '申请时班级角色',
  reviewer_user_id            BIGINT       NULL COMMENT '管理端审批人 sys user id',
  reviewed_at                 DATETIME     NULL,
  reject_reason               VARCHAR(500) NULL,
  tenant_id                   BIGINT       NOT NULL,
  deleted                     TINYINT      NOT NULL DEFAULT 0,
  created_by                  BIGINT       NULL,
  updated_by                  BIGINT       NULL,
  created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tscr_class_status (class_id, status, deleted),
  KEY idx_tscr_applicant (applicant_platform_user_id, status, deleted),
  KEY idx_tscr_schedule_pending (schedule_id, status, deleted),
  KEY idx_tscr_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课表调课申请';
