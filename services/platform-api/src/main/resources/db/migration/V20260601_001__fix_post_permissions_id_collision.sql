-- Fix: post:view/create/update/delete permissions were not inserted
-- because V20260531_002 used IDs 5400-5403 which collided with train:info-list:* permissions.
-- Use IDs 5900-5903 which are confirmed unused.

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5900, 1, 4108, 'post:view', '查看岗位', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5901, 1, 4108, 'post:create', '新增岗位', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5902, 1, 4108, 'post:update', '更新岗位', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5903, 1, 4108, 'post:delete', '删除岗位', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Update sys_role_permission to point to the correct permission IDs
-- (the old entries 705700-705703 point to train:info-list permissions)
UPDATE sys_role_permission
SET permission_id = 5900, updated_at = NOW()
WHERE id = 705700 AND tenant_id = 1 AND role_id = 2000;

UPDATE sys_role_permission
SET permission_id = 5901, updated_at = NOW()
WHERE id = 705701 AND tenant_id = 1 AND role_id = 2000;

UPDATE sys_role_permission
SET permission_id = 5902, updated_at = NOW()
WHERE id = 705702 AND tenant_id = 1 AND role_id = 2000;

UPDATE sys_role_permission
SET permission_id = 5903, updated_at = NOW()
WHERE id = 705703 AND tenant_id = 1 AND role_id = 2000;
