-- COMP-SCORE-02 application closure.
-- Version 20260731.005 follows the verified production maximum successful
-- Flyway version 20260731.004 queried on 2026-07-31.

SET @competition_event_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = 1
    AND code = 'competition_event'
    AND deleted = 0
  LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9973, 1, @competition_event_menu_id, 'competition:score:view',
   '查看竞赛成绩', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9974, 1, @competition_event_menu_id, 'competition:score:manage',
   '修订竞赛成绩来源', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9975, 1, @competition_event_menu_id, 'competition:score:calculate',
   '计算竞赛总分', 'calculate', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9976, 1, @competition_event_menu_id, 'competition:ranking:view',
   '查看竞赛排名快照', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9977, 1, @competition_event_menu_id, 'competition:ranking:generate',
   '生成竞赛排名快照', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_score_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_score_role_permission_id :=
    @next_competition_score_role_permission_id + 1,
  p.tenant_id,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'competition:score:view',
    'competition:score:manage',
    'competition:score:calculate',
    'competition:ranking:view',
    'competition:ranking:generate'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
