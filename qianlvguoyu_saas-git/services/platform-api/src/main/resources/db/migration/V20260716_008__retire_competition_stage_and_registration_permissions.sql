-- 暂停阶段与任务、资格审核模块的权限，待后续重构后重新启用。
UPDATE sys_permission
SET status = 'disabled',
    deleted = 1,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code IN (
    'competition:stage:view',
    'competition:stage:manage',
    'competition:task-template:view',
    'competition:task-template:manage',
    'competition:registration:view',
    'competition:registration:audit'
  );
