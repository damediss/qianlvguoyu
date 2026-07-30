INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (7606, 1, 6301, 'certification.preparation.exam-session.view', '查看认定考试场次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7607, 1, 6301, 'certification.preparation.exam-session.edit', '编辑认定考试场次', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7608, 1, 6301, 'certification.preparation.exam-session.delete', '删除认定考试场次', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7609, 1, 6301, 'certification.preparation.candidate.exam-session.edit', '调整考生参加场次', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) SELECT 910606, 1, 2000, 7606, NOW(), NOW(), 1, 1, 0
  FROM DUAL WHERE EXISTS (SELECT 1 FROM sys_permission WHERE id = 7606 AND deleted = 0);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) SELECT 910607, 1, 2000, 7607, NOW(), NOW(), 1, 1, 0
  FROM DUAL WHERE EXISTS (SELECT 1 FROM sys_permission WHERE id = 7607 AND deleted = 0);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) SELECT 910608, 1, 2000, 7608, NOW(), NOW(), 1, 1, 0
  FROM DUAL WHERE EXISTS (SELECT 1 FROM sys_permission WHERE id = 7608 AND deleted = 0);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) SELECT 910609, 1, 2000, 7609, NOW(), NOW(), 1, 1, 0
  FROM DUAL WHERE EXISTS (SELECT 1 FROM sys_permission WHERE id = 7609 AND deleted = 0);
