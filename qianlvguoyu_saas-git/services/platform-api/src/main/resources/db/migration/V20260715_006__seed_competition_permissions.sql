-- FND-01: permission resources only. No fake competition menu/page is created before FND-05 confirms the menu tree.
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9950, 1, NULL, 'competition:event:view', '查看竞赛赛事', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9951, 1, NULL, 'competition:event:create', '创建竞赛赛事', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9952, 1, NULL, 'competition:event:update', '更新竞赛赛事', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9953, 1, NULL, 'competition:event:publish', '发布竞赛赛事', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9954, 1, NULL, 'competition:enterprise-event:create', '创建企业内部赛', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9955, 1, NULL, 'competition:event-org:manage', '管理竞赛参与企业', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9956, 1, NULL, 'competition:roster:import', '导入竞赛名单', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9957, 1, NULL, 'competition:roster:confirm', '确认竞赛名单', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9958, 1, NULL, 'competition:roster:view', '查看竞赛名单', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9959, 1, NULL, 'competition:registration:view', '查看参赛资格', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9960, 1, NULL, 'competition:registration:audit', '审核参赛资格', 'audit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9961, 1, NULL, 'competition:notice:publish', '发布竞赛通知', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9962, 1, NULL, 'competition:notice:view', '查看竞赛通知', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9963, 1, NULL, 'competition:task-template:manage', '管理竞赛任务模板', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9964, 1, NULL, 'competition:task-template:view', '查看竞赛任务模板', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (800000, 1, 2000, 9950, NOW(), NOW(), 1, 1, 0),
  (800001, 1, 2000, 9951, NOW(), NOW(), 1, 1, 0),
  (800002, 1, 2000, 9952, NOW(), NOW(), 1, 1, 0),
  (800003, 1, 2000, 9953, NOW(), NOW(), 1, 1, 0),
  (800004, 1, 2000, 9954, NOW(), NOW(), 1, 1, 0),
  (800005, 1, 2000, 9955, NOW(), NOW(), 1, 1, 0),
  (800006, 1, 2000, 9956, NOW(), NOW(), 1, 1, 0),
  (800007, 1, 2000, 9957, NOW(), NOW(), 1, 1, 0),
  (800008, 1, 2000, 9958, NOW(), NOW(), 1, 1, 0),
  (800009, 1, 2000, 9959, NOW(), NOW(), 1, 1, 0),
  (800010, 1, 2000, 9960, NOW(), NOW(), 1, 1, 0),
  (800011, 1, 2000, 9961, NOW(), NOW(), 1, 1, 0),
  (800012, 1, 2000, 9962, NOW(), NOW(), 1, 1, 0),
  (800013, 1, 2000, 9963, NOW(), NOW(), 1, 1, 0),
  (800014, 1, 2000, 9964, NOW(), NOW(), 1, 1, 0);

