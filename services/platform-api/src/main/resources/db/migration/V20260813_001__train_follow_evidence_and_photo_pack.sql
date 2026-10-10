-- V20260813_001: 跟班课堂影像 + 课次补强 + 培训照片生成记录
-- 《跟班课堂影像与培训照片材料生成实现方案》E1/E3

ALTER TABLE train_schedule
  ADD COLUMN actual_class_hours VARCHAR(32) NULL COMMENT '实际学时，空则用 class_hours' AFTER class_hours,
  ADD COLUMN evidence_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否须课堂影像 1是0否' AFTER teach_form;

CREATE TABLE IF NOT EXISTS train_follow_evidence (
  id                 BIGINT       NOT NULL COMMENT '雪花 ID',
  class_id           BIGINT       NOT NULL COMMENT 'train_info_class.id',
  schedule_id        BIGINT       NULL COMMENT 'train_schedule.id，优先挂课次',
  session_id         BIGINT       NULL COMMENT '预留 follow_session',
  biz_date           VARCHAR(16)  NULL COMMENT '无课次时 YYYY-MM-DD',
  teach_form_override VARCHAR(32) NULL COMMENT '按日上传时手工理论/实操',
  class_hours_override VARCHAR(32) NULL COMMENT '按日上传时手工学时',
  media_type         VARCHAR(16)  NOT NULL DEFAULT 'photo' COMMENT 'photo/video',
  file_id            BIGINT       NULL COMMENT 'sys/file id',
  file_path          VARCHAR(512) NOT NULL COMMENT '如 /api/files/{id}/download',
  thumb_path         VARCHAR(512) NULL,
  caption            VARCHAR(500) NULL,
  uploader_user_id   BIGINT       NULL,
  uploader_role      VARCHAR(32)  NULL COMMENT 'monitor/head_teacher/...',
  review_status      VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  reviewed_by        BIGINT       NULL,
  reviewed_at        DATETIME     NULL,
  reject_reason      VARCHAR(500) NULL,
  sort_no            INT          NOT NULL DEFAULT 0,
  tenant_id          BIGINT       NOT NULL,
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfe_class_sched (class_id, schedule_id, deleted),
  KEY idx_tfe_class_date (class_id, biz_date, deleted),
  KEY idx_tfe_status (class_id, review_status, deleted),
  KEY idx_tfe_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟班课堂影像证据';

CREATE TABLE IF NOT EXISTS train_follow_photo_pack (
  id                 BIGINT       NOT NULL COMMENT '雪花 ID',
  class_id           BIGINT       NOT NULL,
  file_id            BIGINT       NULL,
  file_path          VARCHAR(512) NOT NULL COMMENT '生成的培训照片.docx',
  title              VARCHAR(200) NULL,
  snapshot_json      MEDIUMTEXT   NULL COMMENT '生成时课次/照片快照',
  generated_at       DATETIME     NOT NULL,
  generated_by       BIGINT       NULL,
  tenant_id          BIGINT       NOT NULL,
  deleted            TINYINT      NOT NULL DEFAULT 0,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tfpp_class (class_id, deleted),
  KEY idx_tfpp_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟班培训照片材料生成记录';
