ALTER TABLE certification_assessor
  ADD COLUMN account_username VARCHAR(64) NULL COMMENT '平板登录账号快照' AFTER user_id,
  ADD COLUMN account_password VARCHAR(128) NULL COMMENT '平板登录密码快照' AFTER account_username;

UPDATE certification_assessor a
JOIN sys_user u ON u.id = a.user_id AND u.tenant_id = a.tenant_id AND u.deleted = 0
SET a.account_username = u.username
WHERE a.account_username IS NULL
  AND a.user_id IS NOT NULL
  AND a.deleted = 0;
