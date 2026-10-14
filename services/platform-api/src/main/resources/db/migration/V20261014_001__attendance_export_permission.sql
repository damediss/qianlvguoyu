-- 跟班考勤「导出考勤表」权限码（方案 docs/04_requirements/跟班考勤导出考勤表方案.md §6）
-- 权限三步链：菜单（train_attendance 已软删——跟班工作台承接入口，菜单复用无需新增）
--           → 权限码（本条）→ sys_role_permission 授权（本条）
--
-- menu_id 置 NULL 的依据：
--   1) 原「考勤管理」菜单（id=4928, tenant=10159）已 deleted=1，现有 4 个 train:attendance:* 权限行
--      挂在该软删菜单下且跟班考勤页权限正常——权限码加载走 sys_role_permission → sys_user_role，
--      不依赖 menu_id（同 V20260922_201__file_permission_codes.sql 注释结论）；
--   2) 前端导出入口在跟班考勤页签内，无新菜单。
--
-- 授权范围：授给 10159 租户下所有持有 train:attendance:view 的角色（与可见者同范围，导出含
-- 身份证号属敏感动作，未授 view 的角色一律不给 export）。
--
-- 取号：2026-10-14 已查两库 MAX(version) = 测试库 20261009.003 / 生产库 20261009.003，
--       共用目录取 V20261014_001（撞号会导致 jar 全环境无法启动）。

-- 1) 权限码（幂等：已存在则跳过）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026101401, 10159, NULL, 'train:attendance:export', '导出考勤表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p
  WHERE p.tenant_id = 10159 AND p.code = 'train:attendance:export' AND p.deleted = 0
);

-- 2) 角色授权（三步链最后一环，缺了接口必 403；幂等）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  20261014020000 + ROW_NUMBER() OVER (ORDER BY rp.role_id),
  10159,
  rp.role_id,
  p_export.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p_view
  ON p_view.id = rp.permission_id
 AND p_view.tenant_id = 10159
 AND p_view.code = 'train:attendance:view'
 AND p_view.deleted = 0
CROSS JOIN sys_permission p_export
WHERE rp.tenant_id = 10159
  AND rp.deleted = 0
  AND p_export.tenant_id = 10159
  AND p_export.code = 'train:attendance:export'
  AND p_export.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = 10159
      AND existing.role_id = rp.role_id
      AND existing.permission_id = p_export.id
      AND existing.deleted = 0
  );
