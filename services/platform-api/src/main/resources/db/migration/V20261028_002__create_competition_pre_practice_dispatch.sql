-- 试题配置与推送记录分开存储；每次提交均创建独立批次，允许同一任务重复推送。
CREATE TABLE IF NOT EXISTS competition_pre_practice_dispatch_batch (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  task_title_snapshot VARCHAR(255) NOT NULL,
  event_id BIGINT NOT NULL,
  event_name_snapshot VARCHAR(255) NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_name_snapshot VARCHAR(255) NOT NULL,
  event_work_type_level_code VARCHAR(16) NOT NULL,
  event_work_type_level_name_snapshot VARCHAR(64) NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_name_snapshot VARCHAR(128) NOT NULL,
  recipient_count INT NOT NULL DEFAULT 0,
  pushed_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_pre_practice_dispatch_scope
    (tenant_id, event_id, event_work_type_id, event_work_type_level_code, pushed_at, deleted),
  KEY idx_pre_practice_dispatch_task (tenant_id, task_id, pushed_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛前练习推送批次';

CREATE TABLE IF NOT EXISTS competition_pre_practice_dispatch_recipient (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  org_name_snapshot VARCHAR(255) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'delivered' COMMENT 'delivered=已进入选手赛事任务列表',
  delivered_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pre_practice_batch_player (tenant_id, batch_id, player_id, deleted),
  KEY idx_pre_practice_recipient_player (tenant_id, player_id, delivered_at, deleted),
  KEY idx_pre_practice_recipient_task (tenant_id, task_id, batch_id, deleted),
  KEY idx_pre_practice_recipient_org (tenant_id, org_id, batch_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛前练习推送选手收件记录';

SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);
SET @pre_practice_directory_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @pre_practice_push_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice_push_management'
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @pre_practice_push_menu_id, @competition_tenant_id, @pre_practice_directory_id,
  'page', 'competition_player_pre_practice_push_management', '推送管理',
  '/competition/player-pre-practice/push-management',
  'competition/CompetitionPlayerPrePracticePushPage', 'competition:player-pre-practice:view',
  'SendOutlined', 2, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, status = 'enabled',
  deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @pre_practice_push_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice_push_management' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @pre_practice_push_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-pre-practice:push'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @pre_practice_push_permission_id, @competition_tenant_id, @pre_practice_push_menu_id,
  'competition:player-pre-practice:push', '推送赛前练习任务', 'push', 'enabled',
  NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1), @competition_tenant_id,
       r.id, @pre_practice_push_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND @pre_practice_directory_id IS NOT NULL
  AND @pre_practice_push_menu_id IS NOT NULL
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission rp
      JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
      WHERE rp.tenant_id = @competition_tenant_id AND rp.role_id = r.id
        AND rp.deleted = 0 AND p.deleted = 0
        AND p.code IN ('competition:player:view', 'competition:event:view')
    )
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1), @competition_tenant_id,
       r.id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
 AND p.code = 'competition:player-pre-practice:push'
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission existing_grant
      JOIN sys_permission existing_permission
        ON existing_permission.tenant_id = existing_grant.tenant_id
       AND existing_permission.id = existing_grant.permission_id
      WHERE existing_grant.tenant_id = @competition_tenant_id
        AND existing_grant.role_id = r.id AND existing_grant.deleted = 0
        AND existing_permission.deleted = 0
        AND existing_permission.code IN (
          'competition:player-pre-practice:view', 'competition:player:view', 'competition:event:view'
        )
    )
  );
