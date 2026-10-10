-- Add a test admin user to the train tenant (id=2062723418189152258)
-- Password hash matches demo/admin (Admin@123456) so the same credential works
INSERT INTO sys_user (id, tenant_id, org_id, username, password_hash, display_name, user_type, status, last_login_at, created_at, updated_at, created_by, updated_by, deleted)
VALUES (999000, 2062723418189152258, NULL, 'trainadmin', '$2y$10$NeoXS685SYXSeLFA.o1Wj.p6nqLS8RixLRcq7WRh4R1EN4kGNsDDW', '培训租户管理员', 'admin', 'enabled', NULL, NOW(), NOW(), 1, 1, 0);

-- Bind PLATFORM_ADMIN role
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted)
VALUES (999000, 2062723418189152258, 999000, 2000, NOW(), NOW(), 1, 1, 0);

SELECT id, tenant_id, username, display_name FROM sys_user WHERE id=999000;
SELECT * FROM sys_user_role WHERE user_id=999000;