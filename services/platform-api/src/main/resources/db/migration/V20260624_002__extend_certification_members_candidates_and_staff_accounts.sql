-- 空库幂等化（踩坑 #34 修复）：存量库这三张表是 PHP 老表缺这些列/索引；
-- 空库中链上建表（V20260603_010 等）已自带，无条件 ADD 必断链（Duplicate column）。
-- 逐列/逐索引存在性守卫；存量库本版本已应用不会重跑（validate-on-migrate=false）。
SET @db := DATABASE();

-- 1) certification_task_member.user_id + idx
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_task_member' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE certification_task_member ADD COLUMN user_id BIGINT NULL COMMENT ''registered platform user id'' AFTER document_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_task_member'
     AND INDEX_NAME = 'idx_certification_member_user') = 0,
  'ALTER TABLE certification_task_member ADD KEY idx_certification_member_user (tenant_id, user_id, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) certification_candidate.admission_ticket_no + idx
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_candidate' AND COLUMN_NAME = 'admission_ticket_no') = 0,
  'ALTER TABLE certification_candidate ADD COLUMN admission_ticket_no VARCHAR(64) NULL COMMENT ''admission ticket number'' AFTER candidate_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_candidate'
     AND INDEX_NAME = 'idx_certification_candidate_admission_ticket') = 0,
  'ALTER TABLE certification_candidate ADD KEY idx_certification_candidate_admission_ticket (tenant_id, task_id, admission_ticket_no, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) certification_info_exam_staff.user_id / account_username + idx
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE certification_info_exam_staff ADD COLUMN user_id BIGINT NULL COMMENT ''registered platform user id'' AFTER tenant_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'account_username') = 0,
  'ALTER TABLE certification_info_exam_staff ADD COLUMN account_username VARCHAR(64) NULL COMMENT ''registered platform username'' AFTER staff_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff'
     AND INDEX_NAME = 'idx_certification_info_exam_staff_user') = 0,
  'ALTER TABLE certification_info_exam_staff ADD KEY idx_certification_info_exam_staff_user (tenant_id, user_id, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) sys_user 字段口径与种子（幂等，保持原样）
ALTER TABLE sys_user MODIFY COLUMN user_type VARCHAR(32) NOT NULL
  COMMENT 'user type: platform_admin, tenant_admin, certification_admin, assessor, normal';

UPDATE sys_user
SET user_type = 'certification_admin'
WHERE tenant_id = 1 AND username = 'certification_admin' AND deleted = 0;

INSERT IGNORE INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  2611, 1, 'CERTIFICATION_ADMIN_NO_REVIEW', '认定管理员（无审核权限）', 'business', 'enabled',
  '认定业务经办账号：可维护项目、筹备与考务信息，不授予审核权限。', NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 920000 + m.id, m.tenant_id, 2611, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.deleted = 0
  AND (m.code LIKE 'certification%' OR m.path LIKE '/certification%');

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 930000 + p.id, p.tenant_id, 2611, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.status = 'enabled'
  AND p.code LIKE 'certification.%'
  AND p.code NOT LIKE '%review%'
  AND p.code NOT LIKE '%initialReview%'
  AND p.code NOT LIKE '%finalReview%';
