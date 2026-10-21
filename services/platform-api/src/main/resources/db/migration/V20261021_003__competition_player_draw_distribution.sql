-- App 选手自主抽签：后台分发任务，选手逐人抽取 1..N 中未占用的出场号。
-- 取号：2026-10-21 查询测试库 max=20261021.002、生产库 max=20261020.004，故使用共用版本 20261021.003。
CREATE TABLE IF NOT EXISTS competition_player_draw_assignment (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  contestant_no VARCHAR(64) NULL,
  player_name VARCHAR(128) NOT NULL,
  org_name VARCHAR(255) NULL,
  draw_status VARCHAR(24) NOT NULL DEFAULT 'pending',
  appearance_order INT NULL,
  drawn_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_draw_assignment_profile
    (tenant_id, task_id, contestant_profile_id, deleted),
  UNIQUE KEY uk_competition_draw_assignment_player
    (tenant_id, task_id, player_id, deleted),
  UNIQUE KEY uk_competition_draw_assignment_order
    (tenant_id, task_id, appearance_order, deleted),
  KEY idx_competition_draw_assignment_player
    (tenant_id, player_id, draw_status, created_at, deleted),
  KEY idx_competition_draw_assignment_task
    (tenant_id, task_id, created_at, deleted)
);

SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);
SET @competition_player_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @draw_parent_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_draw'
  ORDER BY id LIMIT 1
);

-- 原“抽签排序”页面改造成“抽签任务”目录，保留 ID 以继承已有角色授权。
UPDATE sys_menu
SET parent_id = @competition_player_menu_id,
    menu_type = 'directory',
    name = '抽签任务',
    path = '/competition/player-draw',
    component = NULL,
    permission_code = 'competition:player-draw:view',
    icon = 'OrderedListOutlined',
    visible = 1,
    status = 'enabled',
    deleted = 0,
    updated_at = NOW(),
    updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id AND id = @draw_parent_id;

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @draw_distribution_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_draw_distribution'
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));
SET @draw_management_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_draw_management'
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@draw_distribution_menu_id, @competition_tenant_id, @draw_parent_id,
   'page', 'competition_player_draw_distribution', '分发任务',
   '/competition/player-draw/distribution', 'competition/CompetitionPlayerDrawDistributionPage',
   'competition:player-draw:view', 'SendOutlined', 1, 1, 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0),
  (@draw_management_menu_id, @competition_tenant_id, @draw_parent_id,
   'page', 'competition_player_draw_management', '抽签管理',
   '/competition/player-draw/management', 'competition/CompetitionPlayerDrawManagementPage',
   'competition:player-draw:view', 'ProfileOutlined', 2, 1, 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, status = 'enabled',
  deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- 继承旧抽签页面的角色菜单授权，确保“目录 -> 两个子页面”完整可见。
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @competition_tenant_id, rm.role_id, child.menu_id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role_menu rm
CROSS JOIN (
  SELECT @draw_distribution_menu_id AS menu_id
  UNION ALL SELECT @draw_management_menu_id
) child
WHERE rm.tenant_id = @competition_tenant_id
  AND rm.menu_id = @draw_parent_id
  AND rm.deleted = 0
  AND child.menu_id IS NOT NULL;

-- 图片中的三个旧模块下线。历史业务表保留，避免破坏审计记录。
UPDATE sys_menu
SET visible = 0, status = 'disabled', deleted = 1,
    updated_at = NOW(), updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id
  AND code IN ('competition_player_sorting', 'competition_player_classify');

UPDATE sys_permission
SET status = 'disabled', deleted = 1,
    updated_at = NOW(), updated_by = @seed_operator_id
WHERE tenant_id = @competition_tenant_id
  AND (
    code LIKE 'competition:player-sorting:%'
    OR code LIKE 'competition:player-classify:%'
  );
