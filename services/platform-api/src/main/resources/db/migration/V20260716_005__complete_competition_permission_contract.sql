-- FND-06: complete the independent view/manage permissions for competition foundation operations.
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9965, 1, NULL, 'competition:event-org:view', '查看赛事参与企业', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9966, 1, NULL, 'competition:stage:view', '查看赛事赛段', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9967, 1, NULL, 'competition:stage:manage', '管理赛事赛段', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800015, 1, 2000, 9965, NOW(), NOW(), 1, 1, 0),
  (800016, 1, 2000, 9966, NOW(), NOW(), 1, 1, 0),
  (800017, 1, 2000, 9967, NOW(), NOW(), 1, 1, 0);

-- The earlier enterprise-specific create code is superseded by the unified event:create contract.
UPDATE sys_permission
SET status = 'disabled', deleted = 1, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1 AND code = 'competition:enterprise-event:create' AND deleted = 0;
