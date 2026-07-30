-- Disable the unused pre-exam certification feature from menus and permissions.
UPDATE sys_role_menu rm
JOIN sys_menu m
  ON m.id = rm.menu_id
  AND m.tenant_id = rm.tenant_id
SET rm.deleted = 1,
    rm.updated_at = NOW(),
    rm.updated_by = 1
WHERE rm.tenant_id = 1
  AND rm.deleted = 0
  AND m.code = 'certification_pre_exam';

UPDATE sys_role_permission rp
JOIN sys_permission p
  ON p.id = rp.permission_id
  AND p.tenant_id = rp.tenant_id
SET rp.deleted = 1,
    rp.updated_at = NOW(),
    rp.updated_by = 1
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND p.code LIKE 'certification.pre_exam.%';

UPDATE sys_menu
SET status = 'disabled',
    visible = 0,
    deleted = 1,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code = 'certification_pre_exam';

UPDATE sys_permission
SET status = 'disabled',
    deleted = 1,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code LIKE 'certification.pre_exam.%';
