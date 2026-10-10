-- AI 同步学统计权限码修复：V20260811_008 按 code='train_exam-list' AND deleted=0 找菜单，
-- 但该菜单（4904）已 deleted=1，导致 0 行插入、行内按钮不显示。
-- 本迁移改挂到当前生效的「考试计划」菜单（code='kao_shi_ji_hua', deleted=0）下。
-- 版本号依据：DB flyway_schema_history MAX(version)=20260812.001（2026-08-12 查询）

-- 1) 权限码：exam:review-sync:stats:view / exam:review-sync:stats:export
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026081200201,
  1,
  m.id,
  'exam:review-sync:stats:view',
  '查看AI同步学统计',
  'view',
  'enabled',
  NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.code = 'kao_shi_ji_hua' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'exam:review-sync:stats:view' AND p.deleted = 0
  );

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026081200202,
  1,
  m.id,
  'exam:review-sync:stats:export',
  '导出AI同步学统计',
  'export',
  'enabled',
  NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.code = 'kao_shi_ji_hua' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'exam:review-sync:stats:export' AND p.deleted = 0
  );

-- 2) 平台管理员（role_id=2000）兜底授权
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026081200203 + ROW_NUMBER() OVER (ORDER BY p.code),
  1,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.code IN ('exam:review-sync:stats:view', 'exam:review-sync:stats:export')
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- 3) 已有「考试列表查看」权限的角色同步授予统计查看（导出仅管理员，不扩散）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026081200210 + ROW_NUMBER() OVER (ORDER BY rp0.role_id),
  1,
  rp0.role_id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp0
INNER JOIN sys_permission p0 ON p0.id = rp0.permission_id AND p0.deleted = 0
CROSS JOIN sys_permission p
WHERE rp0.tenant_id = 1 AND rp0.deleted = 0
  AND p0.code = 'train:exam-list:view'
  AND p.tenant_id = 1 AND p.code = 'exam:review-sync:stats:view' AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = rp0.role_id AND rp.permission_id = p.id AND rp.deleted = 0
  );
