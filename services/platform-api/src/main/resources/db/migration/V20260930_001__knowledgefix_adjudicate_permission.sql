-- 存疑行人工定夺：权限码 + 平台管理员（role_id=2000）授权（幂等；方案 docs/04_requirements/存疑行人工定夺功能方案.md）
-- 权限三步链：菜单（knowledge_fix 已存在）→ 权限码（本条）→ sys_role_permission 授权（本条）

-- 1) 权限码（挂知识归属修正菜单；重复执行/已存在则跳过）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026093001, 10159, m.id, 'exam:knowledgefix:adjudicate', '存疑行人工定夺', 'adjudicate', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:adjudicate' AND p.deleted = 0
  );

-- 2) 平台管理员授权（role_id=2000，与既有知识修正权限同授权对象；幂等）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026093002, 10159, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:adjudicate' AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );
