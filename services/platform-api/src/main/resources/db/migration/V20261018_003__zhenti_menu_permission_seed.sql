-- 历年真题管理：菜单 + 权限码 + 角色授权（方案 §2.2 / §3.1）
-- 权限三步链：菜单（本脚本第 1 段）→ 权限码（第 2 段）→ sys_role_permission 授权（第 3 段）
-- 缺第三步会让接口 403，联调前先确认本脚本已执行。
--
-- 取号说明同 V20261018_001（两库 MAX 均为 20261014.004）。
-- tenant_id 一律 10159；全部幂等（先查后插），重复执行不产生脏数据。
-- 挂载点：优先“提升/考试管理”目录（ti_sheng_kao_shi_guan_li），兜底取“考试计划”菜单的父节点，
--         与考试计划同级、紧随其后（sort_order = 考试计划 + 1）。

SET @tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @parent_menu_id := COALESCE(
  (SELECT id FROM sys_menu
   WHERE tenant_id = @tenant_id AND code = 'ti_sheng_kao_shi_guan_li' AND deleted = 0
   ORDER BY id LIMIT 1),
  (SELECT parent_id FROM sys_menu
   WHERE tenant_id = @tenant_id AND code = 'kao_shi_ji_hua' AND deleted = 0
   ORDER BY id LIMIT 1),
  0
);

SET @parent_sort := COALESCE(
  (SELECT sort_order FROM sys_menu
   WHERE tenant_id = @tenant_id AND code = 'kao_shi_ji_hua' AND deleted = 0
   ORDER BY id LIMIT 1),
  6
);

-- 1) 菜单
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @zhenti_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_past_papers' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, expanded, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @zhenti_menu_id, @tenant_id, @parent_menu_id, 'page', 'exam_past_papers', '历年真题管理',
  '/exam/past-papers', 'exam/PastPaperManagePage', 'exam:zhenti:view',
  'FilePdfOutlined', @parent_sort + 1, 1, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, expanded = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @zhenti_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_past_papers' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- 2) 权限码：查看 / 管理（上传、编辑、上下架、删除、批量导入）
SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @zhenti_view_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'exam:zhenti:view' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @zhenti_manage_permission_id := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'exam:zhenti:manage' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@zhenti_view_permission_id, @tenant_id, @zhenti_menu_id,
   'exam:zhenti:view', '查看历年真题', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@zhenti_manage_permission_id, @tenant_id, @zhenti_menu_id,
   'exam:zhenti:manage', '管理历年真题', 'manage', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- 3) 角色授权：平台管理员 + 持有“工种试题管理/组卷计划”查看权的教务、运营角色
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
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission rp
      JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
      WHERE rp.tenant_id = @tenant_id AND rp.role_id = r.id
        AND rp.deleted = 0 AND p.deleted = 0
        AND p.code IN ('exam:question:view', 'exam:make-plan:view')
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = r.id
      AND existing.menu_id = @zhenti_menu_id
      AND existing.deleted = 0
  );

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
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission rp
      JOIN sys_permission anchor ON anchor.tenant_id = rp.tenant_id AND anchor.id = rp.permission_id
      WHERE rp.tenant_id = @tenant_id AND rp.role_id = r.id
        AND rp.deleted = 0 AND anchor.deleted = 0
        AND anchor.code IN ('exam:question:view', 'exam:make-plan:view')
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = r.id
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );
