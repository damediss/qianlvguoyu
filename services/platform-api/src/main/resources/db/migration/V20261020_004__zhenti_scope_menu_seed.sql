-- 真题共享范围（考试计划工种绑定长期维护页）：菜单 + 角色授权（方案 v2.1 §8，Q5 已拍板保留）
-- 复用权限码 exam:zhenti:view（查看）/ exam:zhenti:manage（编辑课程绑定），不新增权限码。
-- 挂载：与「历年真题管理」同父目录（ti_sheng_kao_shi_guan_li），紧随其后排在考试计划之后。
-- tenant_id 一律 10159；全部幂等（先查后插 / ON DUPLICATE KEY UPDATE）。

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

-- 排在「历年真题管理」之后：取其 sort_order + 1，兜底取考试计划 + 2
SET @anchor_sort := COALESCE((
  SELECT sort_order FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_past_papers' AND deleted = 0
  ORDER BY id LIMIT 1
), (
  SELECT sort_order + 1 FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'kao_shi_ji_hua' AND deleted = 0
  ORDER BY id LIMIT 1
));

SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @scope_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_zhenti_scope' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, expanded, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @scope_menu_id, @tenant_id, @parent_menu_id, 'page', 'exam_zhenti_scope', '真题共享范围',
  '/exam/past-paper-scope', 'exam/PastPaperScopePage', 'exam:zhenti:view',
  'PartitionOutlined', @anchor_sort + 1, 1, 1, 'enabled', NOW(), NOW(),
  @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, expanded = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @scope_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'exam_zhenti_scope' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- 角色授权：平台管理员 + 已持有历年真题查看权的角色（与真题管理同入口人群）
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @tenant_id, r.id, @scope_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @tenant_id
  AND r.deleted = 0
  AND @scope_menu_id IS NOT NULL
  AND (
    r.id = 2000
    OR EXISTS (
      SELECT 1 FROM sys_role_permission rp
      JOIN sys_permission p ON p.tenant_id = rp.tenant_id AND p.id = rp.permission_id
      WHERE rp.tenant_id = @tenant_id AND rp.role_id = r.id
        AND rp.deleted = 0 AND p.deleted = 0
        AND p.code IN ('exam:zhenti:view', 'exam:zhenti:manage')
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = r.id
      AND existing.menu_id = @scope_menu_id
      AND existing.deleted = 0
  );
