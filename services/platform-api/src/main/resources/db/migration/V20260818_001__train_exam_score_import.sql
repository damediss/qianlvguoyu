-- 人社成绩花名册导入：审计表 + 跟班考试页写入权限

CREATE TABLE IF NOT EXISTS train_exam_score_import_batch (
  id                 BIGINT        NOT NULL COMMENT '雪花 ID',
  tenant_id          BIGINT        NOT NULL,
  operator_user_id   BIGINT        NULL,
  operator_username  VARCHAR(64)   NULL,
  file_name          VARCHAR(255)  NULL,
  file_path          VARCHAR(512)  NULL,
  plan_name          VARCHAR(255)  NULL,
  plan_code          VARCHAR(64)   NULL,
  exam_time_text     VARCHAR(128)  NULL,
  layout             VARCHAR(32)   NULL COMMENT 'RENSHE_ROSTER / FLAT_TEMPLATE',
  class_ids_json     VARCHAR(2000) NULL COMMENT '选定班级 ID JSON 数组',
  train_info_id      BIGINT        NULL,
  sync_exam_plan     TINYINT       NOT NULL DEFAULT 0 COMMENT 'P0 固定 0',
  total_rows         INT           NOT NULL DEFAULT 0,
  matched_count      INT           NOT NULL DEFAULT 0,
  written_count      INT           NOT NULL DEFAULT 0,
  overwritten_count  INT           NOT NULL DEFAULT 0,
  skipped_count      INT           NOT NULL DEFAULT 0,
  unmatched_count    INT           NOT NULL DEFAULT 0,
  deleted            TINYINT       NOT NULL DEFAULT 0,
  created_by         BIGINT        NULL,
  updated_by         BIGINT        NULL,
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tesib_tenant_time (tenant_id, created_at),
  KEY idx_tesib_plan_code (tenant_id, plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩单导入批次';

CREATE TABLE IF NOT EXISTS train_exam_score_import_item (
  id                 BIGINT         NOT NULL COMMENT '雪花 ID',
  batch_id           BIGINT         NOT NULL,
  tenant_id          BIGINT         NOT NULL,
  row_no             INT            NULL,
  id_card            VARCHAR(32)    NULL,
  name               VARCHAR(64)    NULL,
  ticket_no          VARCHAR(64)    NULL,
  theory_score       DECIMAL(10,2)  NULL,
  skill_score        DECIMAL(10,2)  NULL,
  comprehensive_score DECIMAL(10,2) NULL,
  people_id          BIGINT         NULL,
  class_id           BIGINT         NULL,
  result_code        VARCHAR(32)    NOT NULL,
  old_exam_score     DECIMAL(10,2)  NULL,
  new_exam_score     DECIMAL(10,2)  NULL,
  old_real_score     DECIMAL(10,2)  NULL,
  new_real_score     DECIMAL(10,2)  NULL,
  theory_fallback    TINYINT        NOT NULL DEFAULT 0,
  deleted            TINYINT        NOT NULL DEFAULT 0,
  created_by         BIGINT         NULL,
  updated_by         BIGINT         NULL,
  created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tesii_batch (batch_id, deleted),
  KEY idx_tesii_people (people_id),
  KEY idx_tesii_idcard (tenant_id, id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩单导入明细';

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403320,
  1,
  m.id,
  'train:follow-exam:update',
  '导入跟班考试成绩',
  'update',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_follow_exam'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'train:follow-exam:update' AND p.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000854000 + ROW_NUMBER() OVER (ORDER BY rp.role_id),
  rp.tenant_id,
  rp.role_id,
  upd.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_permission rp
INNER JOIN sys_permission viewp
  ON viewp.id = rp.permission_id
 AND viewp.tenant_id = rp.tenant_id
 AND viewp.code = 'train:follow-exam:view'
 AND viewp.deleted = 0
INNER JOIN sys_permission upd
  ON upd.tenant_id = rp.tenant_id
 AND upd.code = 'train:follow-exam:update'
 AND upd.deleted = 0
WHERE rp.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.tenant_id = rp.tenant_id
      AND x.role_id = rp.role_id
      AND x.permission_id = upd.id
      AND x.deleted = 0
  );
