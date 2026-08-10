-- V20260807_001: 班级角色表 train_class_role + 从花名册遗留数据迁移
-- 对应《培训班级五角色落地实现方案》R1
-- 注意：legacy 表多为 utf8mb4_general_ci，新表用 unicode_ci，字符串比较必须显式 COLLATE

CREATE TABLE IF NOT EXISTS train_class_role (
  id                BIGINT       NOT NULL COMMENT '雪花 ID',
  class_id          BIGINT       NOT NULL COMMENT 'train_info_class.id',
  role_code         VARCHAR(32)  NOT NULL COMMENT 'head_teacher/follow_staff/lead_teacher/assistant/monitor',
  subject_type      VARCHAR(16)  NOT NULL COMMENT 'platform_user/people/teacher_bank',
  subject_id        BIGINT       NOT NULL COMMENT '主体 ID',
  platform_user_id  BIGINT       NULL COMMENT 'App 鉴权用 platform_user.id',
  people_id         BIGINT       NULL COMMENT '关联 train_people_list.id',
  display_name      VARCHAR(64)  NULL COMMENT '展示姓名',
  phone             VARCHAR(32)  NULL COMMENT '手机号冗余',
  start_date        DATE         NULL COMMENT '起任',
  end_date          DATE         NULL COMMENT '卸任',
  status            VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/ended',
  tenant_id         BIGINT       NOT NULL,
  deleted           TINYINT      NOT NULL DEFAULT 0,
  created_by        BIGINT       NULL,
  updated_by        BIGINT       NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tcr_class_status (class_id, status, deleted),
  KEY idx_tcr_platform_user (platform_user_id, status, deleted),
  KEY idx_tcr_role (class_id, role_code, status),
  KEY idx_tcr_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级岗位角色指派';

-- staff → follow_staff
INSERT INTO train_class_role (
  id, class_id, role_code, subject_type, subject_id, platform_user_id, people_id,
  display_name, phone, status, tenant_id, deleted, created_at, updated_at
)
SELECT
  202608071000000000 + p.id,
  p.train_class_id,
  'follow_staff',
  CASE WHEN p.user_id IS NOT NULL AND p.user_id > 0 THEN 'teacher_bank' ELSE 'people' END,
  COALESCE(NULLIF(p.user_id, 0), p.id),
  (
    SELECT u.id FROM platform_user u
    WHERE u.deleted = 0
      AND (
        (p.id_card IS NOT NULL AND TRIM(p.id_card) <> ''
          AND u.idcard COLLATE utf8mb4_unicode_ci = p.id_card COLLATE utf8mb4_unicode_ci)
        OR (p.phone IS NOT NULL AND TRIM(p.phone) <> ''
          AND u.phone COLLATE utf8mb4_unicode_ci = p.phone COLLATE utf8mb4_unicode_ci)
      )
    ORDER BY u.id ASC
    LIMIT 1
  ),
  p.id,
  p.realname,
  p.phone,
  'active',
  COALESCE(p.tenant_id, 1),
  0,
  NOW(),
  NOW()
FROM train_people_list p
WHERE p.deleted = 0
  AND p.type COLLATE utf8mb4_unicode_ci = 'staff'
  AND p.train_class_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM train_class_role r
    WHERE r.class_id = p.train_class_id
      AND r.role_code COLLATE utf8mb4_unicode_ci = 'follow_staff'
      AND r.people_id = p.id
      AND r.deleted = 0
  );

-- inCharge → head_teacher
INSERT INTO train_class_role (
  id, class_id, role_code, subject_type, subject_id, platform_user_id, people_id,
  display_name, phone, status, tenant_id, deleted, created_at, updated_at
)
SELECT
  202608072000000000 + p.id,
  p.train_class_id,
  'head_teacher',
  'people',
  p.id,
  (
    SELECT u.id FROM platform_user u
    WHERE u.deleted = 0
      AND (
        (p.id_card IS NOT NULL AND TRIM(p.id_card) <> ''
          AND u.idcard COLLATE utf8mb4_unicode_ci = p.id_card COLLATE utf8mb4_unicode_ci)
        OR (p.phone IS NOT NULL AND TRIM(p.phone) <> ''
          AND u.phone COLLATE utf8mb4_unicode_ci = p.phone COLLATE utf8mb4_unicode_ci)
      )
    ORDER BY u.id ASC
    LIMIT 1
  ),
  p.id,
  p.realname,
  p.phone,
  'active',
  COALESCE(p.tenant_id, 1),
  0,
  NOW(),
  NOW()
FROM train_people_list p
WHERE p.deleted = 0
  AND p.type COLLATE utf8mb4_unicode_ci = 'inCharge'
  AND p.train_class_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM train_class_role r
    WHERE r.class_id = p.train_class_id
      AND r.role_code COLLATE utf8mb4_unicode_ci = 'head_teacher'
      AND r.people_id = p.id
      AND r.deleted = 0
  );

-- is_leader=1 student → monitor
INSERT INTO train_class_role (
  id, class_id, role_code, subject_type, subject_id, platform_user_id, people_id,
  display_name, phone, status, tenant_id, deleted, created_at, updated_at
)
SELECT
  202608073000000000 + p.id,
  p.train_class_id,
  'monitor',
  'people',
  p.id,
  p.platform_user_id,
  p.id,
  p.realname,
  p.phone,
  'active',
  COALESCE(p.tenant_id, 1),
  0,
  NOW(),
  NOW()
FROM train_people_list p
WHERE p.deleted = 0
  AND p.type COLLATE utf8mb4_unicode_ci = 'student'
  AND p.is_leader = 1
  AND p.train_class_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM train_class_role r
    WHERE r.class_id = p.train_class_id
      AND r.role_code COLLATE utf8mb4_unicode_ci = 'monitor'
      AND r.people_id = p.id
      AND r.deleted = 0
  );
