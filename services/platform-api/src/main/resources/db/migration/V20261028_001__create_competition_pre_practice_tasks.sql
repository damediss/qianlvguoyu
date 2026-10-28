-- 赛前练习先固化配置任务和抽中的课程题目快照；选手端分发由后续阶段实现。
CREATE TABLE IF NOT EXISTS competition_pre_practice_task (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_name_snapshot VARCHAR(255) NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_name_snapshot VARCHAR(255) NOT NULL,
  event_work_type_level_code VARCHAR(16) NOT NULL,
  event_work_type_level_name_snapshot VARCHAR(64) NOT NULL,
  stage_id BIGINT NOT NULL,
  stage_name_snapshot VARCHAR(128) NOT NULL,
  title VARCHAR(255) NOT NULL,
  source_scope_json JSON NOT NULL,
  question_type_counts_json JSON NOT NULL,
  question_count INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL DEFAULT 'configured' COMMENT 'configured=配置完成，后续可扩展任务生命周期',
  delivery_status VARCHAR(24) NOT NULL DEFAULT 'not_pushed' COMMENT '预留选手端分发状态；本期不执行分发',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_competition_pre_practice_event (tenant_id, event_id, created_at, deleted),
  KEY idx_competition_pre_practice_scope
    (tenant_id, event_work_type_id, event_work_type_level_code, stage_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛前练习任务配置';

CREATE TABLE IF NOT EXISTS competition_pre_practice_task_question (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  source_question_id BIGINT NOT NULL COMMENT 'exam_exam_detail.id',
  source_course_id INT NOT NULL,
  source_work_type_id INT NOT NULL,
  source_level TINYINT NOT NULL COMMENT 'exam_course.level: 0-4',
  question_type VARCHAR(32) NOT NULL,
  sort_order INT NOT NULL,
  question_snapshot_json JSON NOT NULL COMMENT '配置生成时的课程试题快照，供后续选手端分发使用',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_pre_practice_question
    (tenant_id, task_id, source_question_id, deleted),
  KEY idx_competition_pre_practice_question_task
    (tenant_id, task_id, sort_order, deleted),
  KEY idx_competition_pre_practice_question_source
    (tenant_id, source_question_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='赛前练习任务课程试题快照';

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

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @pre_practice_directory_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_player_pre_practice'
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));
SET @pre_practice_config_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice_question_config'
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@pre_practice_directory_id, @competition_tenant_id, @competition_player_menu_id,
   'directory', 'competition_player_pre_practice', '赛前练习',
   '/competition/player-pre-practice', NULL, 'competition:player-pre-practice:view',
   'ReadOutlined', 5, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@pre_practice_config_menu_id, @competition_tenant_id, @pre_practice_directory_id,
   'page', 'competition_player_pre_practice_question_config', '试题配置',
   '/competition/player-pre-practice/question-config',
   'competition/CompetitionPlayerPrePracticePage', 'competition:player-pre-practice:view',
   'SettingOutlined', 1, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, status = 'enabled',
  deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @pre_practice_directory_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @pre_practice_config_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id
    AND code = 'competition_player_pre_practice_question_config' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @pre_practice_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-pre-practice:view'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @pre_practice_create_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @competition_tenant_id AND code = 'competition:player-pre-practice:create'
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@pre_practice_view_permission_id, @competition_tenant_id, @pre_practice_config_menu_id,
   'competition:player-pre-practice:view', '查看赛前练习试题配置', 'view', 'enabled',
   NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@pre_practice_create_permission_id, @competition_tenant_id, @pre_practice_config_menu_id,
   'competition:player-pre-practice:create', '生成赛前练习任务', 'create', 'enabled',
   NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- 继承已有竞赛选手管理角色的菜单授权，按查看和任务创建权限分配到角色。
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @competition_tenant_id, r.id, child.menu_id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
CROSS JOIN (
  SELECT @pre_practice_directory_id AS menu_id
  UNION ALL SELECT @pre_practice_config_menu_id
) child
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND child.menu_id IS NOT NULL
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
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.code = 'competition:player-pre-practice:view'
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
        AND existing_permission.code IN ('competition:player:view', 'competition:event:view')
    )
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @competition_tenant_id, r.id, p.id, NOW(), NOW(),
       @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND p.code = 'competition:player-pre-practice:create'
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
        AND existing_permission.code IN ('competition:player:create', 'competition:stage:manage')
    )
  );
