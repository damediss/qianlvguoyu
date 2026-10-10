-- 20260904.003 seeded the competition menu records with the historical tenant
-- placeholder (1). Runtime menu loading is tenant-scoped, so align the records
-- with the existing competition root menu and repair the parent links.
SET @competition_scope_tenant_id := COALESCE(
  (
    SELECT tenant_id
    FROM sys_menu
    WHERE code = 'competition_foundation'
      AND parent_id = 0
      AND deleted = 0
    ORDER BY id
    LIMIT 1
  ),
  (
    SELECT tenant_id
    FROM sys_role
    WHERE id = 2000
      AND deleted = 0
    ORDER BY id
    LIMIT 1
  ),
  1
);

SET @competition_root_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_foundation'
    AND parent_id = 0
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

UPDATE sys_menu
SET tenant_id = @competition_scope_tenant_id,
    parent_id = CASE
      WHEN code IN ('competition_question_bank', 'competition_make_exam_plan')
        THEN COALESCE(@competition_root_menu_id, 0)
      WHEN code IN ('competition_theory_question', 'competition_practical_question')
        THEN 202609040301
      ELSE parent_id
    END
WHERE id IN (202609040301, 202609040302, 202609040303, 202609040304)
  AND deleted = 0;

UPDATE sys_permission p
INNER JOIN sys_menu m ON m.id = p.menu_id
SET p.tenant_id = m.tenant_id
WHERE p.id BETWEEN 202609040401 AND 202609040410
  AND p.deleted = 0;

UPDATE sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id
SET rm.tenant_id = m.tenant_id
WHERE rm.menu_id BETWEEN 202609040301 AND 202609040304
  AND rm.deleted = 0;

UPDATE sys_role_permission rp
INNER JOIN sys_permission p ON p.id = rp.permission_id
SET rp.tenant_id = p.tenant_id
WHERE rp.permission_id BETWEEN 202609040401 AND 202609040410
  AND rp.deleted = 0;
