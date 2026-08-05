-- Split event operations out of the event-creation menu so they can be reached directly.

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);

SET @competition_notice_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_notice' AND deleted = 0
  LIMIT 1
), (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu));

SET @competition_theory_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_theory_exam' AND deleted = 0
  LIMIT 1
), (SELECT COALESCE(MAX(id), 0) + 2 FROM sys_menu));

SET @competition_score_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_score_ranking' AND deleted = 0
  LIMIT 1
), (SELECT COALESCE(MAX(id), 0) + 3 FROM sys_menu));

INSERT INTO sys_menu (id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (@competition_notice_menu_id, 1, @competition_root_menu_id, 'page', 'competition_notice', '公告设置', '/competition/notices', 'competition/CompetitionEventManagementPage', 'competition:notice:view', 'BellOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (@competition_theory_menu_id, 1, @competition_root_menu_id, 'page', 'competition_theory_exam', '理论考试', '/competition/theory-exams', 'competition/CompetitionEventManagementPage', 'competition:theory-paper:view', 'ReadOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (@competition_score_menu_id, 1, @competition_root_menu_id, 'page', 'competition_score_ranking', '成绩与排名', '/competition/scores', 'competition/CompetitionEventManagementPage', 'competition:score:view', 'TrophyOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), name = VALUES(name), path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code), icon = VALUES(icon), sort_order = VALUES(sort_order), visible = VALUES(visible), status = VALUES(status), deleted = 0, updated_at = NOW(), updated_by = 1;

UPDATE sys_permission
SET menu_id = @competition_notice_menu_id, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1 AND deleted = 0 AND code IN ('competition:notice:view', 'competition:notice:publish');

UPDATE sys_permission
SET menu_id = @competition_theory_menu_id, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1 AND deleted = 0 AND code IN (
  'competition:theory-paper:view', 'competition:theory-paper:manage',
  'competition:theory-schedule:view', 'competition:theory-schedule:manage', 'competition:theory-schedule:publish', 'competition:theory-roster:export',
  'competition:theory-monitor:view', 'competition:theory-monitor:force-offline', 'competition:theory-monitor:force-submit', 'competition:theory-monitor:restore-absence',
  'competition:theory-result:view'
);

UPDATE sys_permission
SET menu_id = @competition_score_menu_id, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1 AND deleted = 0 AND code IN (
  'competition:score:view', 'competition:score:manage', 'competition:score:calculate',
  'competition:ranking:view', 'competition:ranking:generate'
);

SET @next_competition_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT @next_competition_role_menu_id := @next_competition_role_menu_id + 1, rp.tenant_id, rp.role_id, @competition_notice_menu_id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id AND p.deleted = 0
WHERE p.code = 'competition:notice:view';

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT @next_competition_role_menu_id := @next_competition_role_menu_id + 1, rp.tenant_id, rp.role_id, @competition_theory_menu_id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id AND p.deleted = 0
WHERE p.code IN ('competition:theory-paper:view', 'competition:theory-schedule:view', 'competition:theory-monitor:view', 'competition:theory-result:view');

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT @next_competition_role_menu_id := @next_competition_role_menu_id + 1, rp.tenant_id, rp.role_id, @competition_score_menu_id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id AND p.deleted = 0
WHERE p.code IN ('competition:score:view', 'competition:ranking:view');
