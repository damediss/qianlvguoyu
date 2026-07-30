ALTER TABLE certification_task_member
  ADD COLUMN user_id BIGINT NULL COMMENT 'registered platform user id' AFTER document_id,
  ADD KEY idx_certification_member_user (tenant_id, user_id, deleted);

ALTER TABLE certification_candidate
  ADD COLUMN admission_ticket_no VARCHAR(64) NULL COMMENT 'admission ticket number' AFTER candidate_no,
  ADD KEY idx_certification_candidate_admission_ticket (tenant_id, task_id, admission_ticket_no, deleted);

ALTER TABLE certification_info_exam_staff
  ADD COLUMN user_id BIGINT NULL COMMENT 'registered platform user id' AFTER tenant_id,
  ADD COLUMN account_username VARCHAR(64) NULL COMMENT 'registered platform username' AFTER staff_name,
  ADD KEY idx_certification_info_exam_staff_user (tenant_id, user_id, deleted);

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
