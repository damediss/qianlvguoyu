-- 操作技能试题生成管理：菜单 + 权限三步链（坑#8）
-- 方案：docs/04_requirements/操作技能试题生成管理方案.md v1.1（T1 迁移 3，决策 C9）
-- 菜单位置：智能课程材料管理（2087736644369305601）下、"理论知识试题生成管理"（sort_order=2）之后
-- 租户：种子一律 10159（千铝工投，运行租户；勿照抄旧文件的 tenant_id=1）
-- 版本号依据（2026-09-10 双库查询）：gjjddb2026(测试) MAX=20260909.004；gjjddbpro(生产) MAX=20260909.003，
-- 本组迁移取 20260910.010~013（踩坑 #7）。

-- 1) sys_menu
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026091000101, 10159, 2087736644369305601, 'menu',
  'skill_question_gen', '操作技能试题生成管理',
  '/cao-zuo-ji-neng-ti-sheng-cheng', 'zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage',
  'exam:skill:blueprint:view', 'SolutionOutlined', 3, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
);

-- 2) sys_permission 11 条
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000201, 10159, m.id, 'exam:skill:blueprint:view', '查看操作技能细目表批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:blueprint:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000202, 10159, m.id, 'exam:skill:blueprint:import', '导入操作技能细目表', 'import', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:blueprint:import' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000203, 10159, m.id, 'exam:skill:gen:view', '查看操作技能生成批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:gen:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000204, 10159, m.id, 'exam:skill:gen:submit', '提交操作技能生成批次', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:gen:submit' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000205, 10159, m.id, 'exam:skill:gen:pause', '暂停/继续/重试操作技能生成批次', 'pause', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:gen:pause' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000206, 10159, m.id, 'exam:skill:gen:export', '导出操作技能试题表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:gen:export' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000207, 10159, m.id, 'exam:skill:draft:view', '查看操作技能试题草稿', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:draft:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000208, 10159, m.id, 'exam:skill:draft:edit', '编辑操作技能试题草稿', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:draft:edit' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000209, 10159, m.id, 'exam:skill:draft:review', '审核操作技能试题草稿', 'review', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:draft:review' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000210, 10159, m.id, 'exam:skill:draft:publish', '操作技能草稿入库', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:draft:publish' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000211, 10159, m.id, 'exam:skill:draft:export', '导出操作技能试题草稿', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:skill:draft:export' AND p.deleted = 0);

-- 3) sys_role_menu：平台管理员（role_id=2000）挂接菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091000301, 10159, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'skill_question_gen' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 10159 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 4) sys_role_permission：平台管理员授予全部权限码
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026091000401 + ROW_NUMBER() OVER (ORDER BY p.code),
  10159,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code IN (
    'exam:skill:blueprint:view', 'exam:skill:blueprint:import',
    'exam:skill:gen:view', 'exam:skill:gen:submit', 'exam:skill:gen:pause', 'exam:skill:gen:export',
    'exam:skill:draft:view', 'exam:skill:draft:edit', 'exam:skill:draft:review',
    'exam:skill:draft:publish', 'exam:skill:draft:export'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );
