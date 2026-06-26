-- Add "人才识别" (rencaishibie embed) under knowledge directory
-- Single entry point: knowledge → 人才识别 (iframe → /rencai/)
-- Hide zhixun_talent_identification to avoid duplicate entries

-- 1. New page under knowledge (parent_id=4600)
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4603, 1, 4600, 'page', 'knowledge_rencaishibie', '人才识别', '/knowledge/rencaishibie', 'knowledge/RencaishibieEmbed', 'knowledge.rencaishibie:view', 'TeamOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
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
  updated_at = NOW(),
  updated_by = 1;

-- 2. Permission
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5117, 1, 4603, 'knowledge.rencaishibie:view', '查看人才识别', 'view', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1;

-- 3. Grant to PLATFORM_ADMIN (role_id=2000)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604603, 1, 2000, 4603, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705117, 1, 2000, 5117, NOW(), NOW(), 1, 1, 0);

-- 4. Grant to CERTIFICATION_ADMIN (role_id=2604)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (999403, 1, 2604, 4603, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (999517, 1, 2604, 5117, NOW(), NOW(), 1, 1, 0);

-- 5. Grant to CERTIFICATION_ADMIN_NO_REVIEW (role_id=2611)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (999463, 1, 2611, 4603, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (999577, 1, 2611, 5117, NOW(), NOW(), 1, 1, 0);

-- 6. Hide zhixun talent identification entry from sidebar
UPDATE sys_menu
SET visible = 0,
    status = 'disabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'zhixun_talent_identification'
  AND deleted = 0;
