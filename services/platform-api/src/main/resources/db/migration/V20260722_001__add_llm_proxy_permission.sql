-- LLM 代理接口权限码 ai:llm:proxy + 授予平台管理员
-- 1. 建权限码（挂"人才识别"菜单 menu_id=4603）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (660007, 1, 4603, 'ai:llm:proxy', '调用LLM代理接口', 'proxy', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

-- 2. 授予平台管理员（role_id=2000, tenant_id=1）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6600307, 1, 2000, 660007, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;



INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (660008, 1, 4603, 'rencai:task:create', '创建人才识别', 'proxy', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (660009, 1, 4603, 'rencai:task:delete', '删除人才识别', 'proxy', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (660010, 1, 4603, 'rencai:task:download', '下载人才识别', 'proxy', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;


INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6600309, 1, 2000, 660008, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6600310, 1, 2000, 660009, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6600311, 1, 2000, 660010, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;