INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6944, 1, 6301, 'certification.project.document.submit', 'Submit certification document workflow', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6945, 1, 6301, 'certification.project.document.initialReview', 'Initial review certification document', 'approve', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6946, 1, 6301, 'certification.project.document.finalReview', 'Final review certification document', 'approve', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6947, 1, 6301, 'certification.project.document.workflowView', 'View certification document workflow', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  2604, 1, 'CERTIFICATION_ADMIN', '认定管理员', 'business', 'enabled',
  '认定模块开发账号角色，仅授予认定项目、认定筹备和文档工作流权限',
  NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_user (
  id, tenant_id, org_id, username, password_hash, display_name, user_type, phone, email, avatar_file_id,
  status, last_login_at, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  1011, 1, 100, 'certification_admin',
  '$2y$10$NeoXS685SYXSeLFA.o1Wj.p6nqLS8RixLRcq7WRh4R1EN4kGNsDDW',
  '认定管理员', 'admin', NULL, NULL, NULL, 'enabled', NULL, NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_user_role (
  id, tenant_id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  3011, 1, 1011, 2604, NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (999100, 1, 2604, 6300, NOW(), NOW(), 1, 1, 0),
  (999101, 1, 2604, 6301, NOW(), NOW(), 1, 1, 0),
  (999102, 1, 2604, 6310, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 907100 + p.id - 6900, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.project',
    'certification.project.task.select',
    'certification.project.task.list',
    'certification.project.task.create',
    'certification.project.task.delete',
    'certification.project.task.flow',
    'certification.project.charter.view',
    'certification.project.charter.edit',
    'certification.project.charter.submit',
    'certification.project.members.view',
    'certification.project.members.edit',
    'certification.project.members.submit',
    'certification.project.wbs.view',
    'certification.project.wbs.edit',
    'certification.project.wbs.submit',
    'certification.project.schedule.view',
    'certification.project.schedule.edit',
    'certification.project.schedule.submit',
    'certification.project.risk.view',
    'certification.project.risk.edit',
    'certification.project.risk.submit',
    'certification.preparation.task.view',
    'certification.preparation.candidate.view',
    'certification.preparation.candidate.import',
    'certification.preparation.candidate.edit',
    'certification.preparation.candidate.delete',
    'certification.preparation.document.view',
    'certification.preparation.document.upload',
    'certification.preparation.document.review',
    'certification.preparation.material.view',
    'certification.preparation.material.upload',
    'certification.preparation.material.generate',
    'certification.preparation.material.download',
    'certification.preparation.site.view',
    'certification.preparation.site.edit',
    'certification.preparation.site.assign',
    'certification.preparation.assessor.view',
    'certification.preparation.assessor.edit',
    'certification.preparation.assessor.assign',
    'certification.preparation.assessor.dispatch',
    'certification.preparation.assessor.workbench',
    'certification.preparation.assessor.score',
    'certification.preparation.assessor.review',
    'certification.preparation.complete',
    'certification.project.document.submit',
    'certification.project.document.initialReview',
    'certification.project.document.finalReview',
    'certification.project.document.workflowView'
  );
