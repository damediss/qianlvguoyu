-- 理论知识试题生成管理：菜单 + 权限三步链（坑#8）
-- 方案：docs/04_requirements/理论知识试题生成管理方案.md v1.2（T1 迁移 3，确认项 C2）
-- 菜单位置：智能课程材料管理（2087736644369305601）下、"细目表管理"（sort_order=1）之后
-- 租户：种子一律 10159（千铝工投，运行租户；admin/sys_user.tenant_id=10159，勿照抄旧文件的 tenant_id=1）
-- 版本号依据（2026-09-04 双库查询）：gjjddbpro(生产) MAX=20260831.001；
-- gjjddb2026(测试) 已被 train_certificate 工作流占用 20260904.001/.002，
-- 本组迁移取 20260904.010~013（踩坑 #7）。原 20260904_002 编号从未在任何库应用。

-- 1) sys_menu（父目录已人工建好且 deleted=0 已核实，此处按父菜单存在性条件插入）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026090400101, 10159, 2087736644369305601, 'menu',
  'theory_question_gen', '理论知识试题生成管理',
  '/li-lun-shi-ti-sheng-cheng', 'zhi_neng_ke_cheng_cai_liao_guan_li/TheoryQuestionGenPage',
  'exam:theory:blueprint:view', 'FileDoneOutlined', 2, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
);

-- 2) sys_permission 11 条
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400201, 10159, m.id, 'exam:theory:blueprint:view', '查看理论细目表批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:blueprint:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400202, 10159, m.id, 'exam:theory:blueprint:import', '导入理论细目表', 'import', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:blueprint:import' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400203, 10159, m.id, 'exam:theory:gen:view', '查看生成批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:gen:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400204, 10159, m.id, 'exam:theory:gen:submit', '提交生成批次', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:gen:submit' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400205, 10159, m.id, 'exam:theory:gen:pause', '暂停/继续/重试生成批次', 'pause', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:gen:pause' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400206, 10159, m.id, 'exam:theory:gen:export', '导出试题表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:gen:export' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400207, 10159, m.id, 'exam:theory:draft:view', '查看试题草稿', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:draft:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400208, 10159, m.id, 'exam:theory:draft:edit', '编辑试题草稿', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:draft:edit' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400209, 10159, m.id, 'exam:theory:draft:review', '审核试题草稿', 'review', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:draft:review' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400210, 10159, m.id, 'exam:theory:draft:publish', '草稿入库', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:draft:publish' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400211, 10159, m.id, 'exam:theory:draft:export', '导出试题草稿', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:theory:draft:export' AND p.deleted = 0);

-- 3) sys_role_menu：平台管理员（role_id=2000）挂接菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026090400301, 10159, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'theory_question_gen' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 10159 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 4) sys_role_permission：平台管理员授予全部权限码
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026090400401 + ROW_NUMBER() OVER (ORDER BY p.code),
  10159,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code IN (
    'exam:theory:blueprint:view', 'exam:theory:blueprint:import',
    'exam:theory:gen:view', 'exam:theory:gen:submit', 'exam:theory:gen:pause', 'exam:theory:gen:export',
    'exam:theory:draft:view', 'exam:theory:draft:edit', 'exam:theory:draft:review',
    'exam:theory:draft:publish', 'exam:theory:draft:export'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );
