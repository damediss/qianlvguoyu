-- 试题 FAQ 转换菜单权限（6 个权限码） + 授予平台管理员
-- 1. 创建权限码
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (660001, 1, 6600, 'exam:faq:view',     '查看试题FAQ知识管理', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (660002, 1, 6600, 'exam:faq:generate', '执行FAQ转换',        'generate', 'enabled', NOW(), NOW(), 1, 1, 0),
  (660003, 1, 6600, 'exam:faq:pause',    '暂停/恢复批次',       'pause',  'enabled', NOW(), NOW(), 1, 1, 0),
  (660004, 1, 6600, 'exam:faq:retry',    '重试失败记录',        'retry',  'enabled', NOW(), NOW(), 1, 1, 0),
  (660005, 1, 6600, 'exam:faq:export',   '导出FAQ JSON',       'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (660006, 1, 6600, 'exam:faq:config',   '配置LLM/Webhook',    'config', 'enabled', NOW(), NOW(), 1, 1, 0)
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
  (6600301, 1, 2000, 660001, NOW(), NOW(), 1, 1, 0),
  (6600302, 1, 2000, 660002, NOW(), NOW(), 1, 1, 0),
  (6600303, 1, 2000, 660003, NOW(), NOW(), 1, 1, 0),
  (6600304, 1, 2000, 660004, NOW(), NOW(), 1, 1, 0),
  (6600305, 1, 2000, 660005, NOW(), NOW(), 1, 1, 0),
  (6600306, 1, 2000, 660006, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
