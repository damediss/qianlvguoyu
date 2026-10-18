-- 历年真题：补齐「持有考试计划查看权」角色的授权（幂等）
--
-- 背景：历年真题页的考试项目下拉复用 /training/exam-lists，该接口要求
-- train:exam-list:view；V20261018_003 仅按 exam:question:view / exam:make-plan:view
-- 找教务/运营角色，若角色没有考试计划查看权，进页面会拿不到下拉数据。
-- 本迁移把「已有考试计划查看权」的角色补授历年真题菜单 + 两个权限码。
--
-- 取号：测试库/生产库 MAX 均为 20261018.003（本文件写入时），故取 004。
-- 全部先查后插，重复执行不产生脏数据；tenant_id 一律 10159。

SET @tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @zhenti_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_past_papers' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- 1) 角色-菜单
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @tenant_id, r.id, @zhenti_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @tenant_id
  AND r.deleted = 0
  AND @zhenti_menu_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM sys_role_permission rp
    JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
    WHERE rp.tenant_id = @tenant_id AND rp.role_id = r.id
      AND rp.deleted = 0 AND p.deleted = 0
      AND p.code = 'train:exam-list:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = r.id
      AND existing.menu_id = @zhenti_menu_id
      AND existing.deleted = 0
  );

-- 2) 角色-权限码
SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @tenant_id, r.id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = @tenant_id
 AND p.deleted = 0
 AND p.code IN ('exam:zhenti:view', 'exam:zhenti:manage')
WHERE r.tenant_id = @tenant_id
  AND r.deleted = 0
  AND EXISTS (
    SELECT 1 FROM sys_role_permission rp
    JOIN sys_permission anchor ON anchor.tenant_id = rp.tenant_id AND anchor.id = rp.permission_id
    WHERE rp.tenant_id = @tenant_id AND rp.role_id = r.id
      AND rp.deleted = 0 AND anchor.deleted = 0
      AND anchor.code = 'train:exam-list:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = r.id
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );
