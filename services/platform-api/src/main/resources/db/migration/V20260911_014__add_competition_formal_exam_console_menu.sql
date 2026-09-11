-- 将理论考试正式比赛运行控制台作为竞赛管理下的独立同级入口。
-- 运行权限从历史兜底菜单迁移到该入口，避免继续挂在赛事创建菜单下。

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

SET @competition_formal_exam_menu_id := COALESCE(
  (
    SELECT id
    FROM sys_menu
    WHERE tenant_id = @competition_scope_tenant_id
      AND code = 'competition_formal_exam'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
  ),
  (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_formal_exam_menu_id,
  @competition_scope_tenant_id,
  COALESCE(@competition_root_menu_id, 0),
  'page',
  'competition_formal_exam',
  '理论比赛控制台',
  '/competition/formal-exam',
  'competition/CompetitionEventManagementPage',
  'competition:exam-session:view',
  'PlayCircleOutlined',
  5,
  1,
  'enabled',
  NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET sort_order = 6,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_score_ranking'
  AND deleted = 0;

UPDATE sys_permission
SET menu_id = @competition_formal_exam_menu_id,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND deleted = 0
  AND code IN (
    'competition:exam-session:view',
    'competition:exam-session:prepare',
    'competition:exam-session:start',
    'competition:exam-session:force-submit',
    'competition:exam-session:score'
  );

SET @next_competition_formal_exam_role_menu_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_menu
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_role_menu_id :=
    @next_competition_formal_exam_role_menu_id + 1,
  rp.tenant_id,
  rp.role_id,
  @competition_formal_exam_menu_id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p
  ON p.id = rp.permission_id
 AND p.tenant_id = rp.tenant_id
 AND p.code = 'competition:exam-session:view'
 AND p.deleted = 0
WHERE rp.tenant_id = @competition_scope_tenant_id
  AND rp.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.menu_id = @competition_formal_exam_menu_id
      AND existing.deleted = 0
  );
