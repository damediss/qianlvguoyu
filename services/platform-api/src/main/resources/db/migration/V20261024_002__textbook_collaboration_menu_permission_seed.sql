-- 职业标准驱动教材生成（二期编写协同）：菜单 + 权限码 + 业务角色种子
-- 方案：docs/04_requirements/职业标准驱动教材生成_二期编写协同实施设计.md v1.1 §4.1/§5.8
-- 权限三步链：菜单（第 1 段）→ 权限码（第 2 段）→ sys_role_menu + sys_role_permission 授权（第 3 段），
-- 缺第三步接口 403。
--
-- 取号（2026-10-24 双库核查）：测试库 gjjddb2026（114.132.67.185:3309）MAX(version)=20261022.001
-- （真实查询确认，已预留 20261024.001 给二期建表迁移）；生产库 gjjddbpro（43.138.163.7:3308）
-- 以 gjjd + 测试库同密码只读连接被拒（ERROR 1045 Access denied，2026-10-24 实测），无法核实
-- MAX(version)。本迁移取 20261024.002（同日第 2 个共用迁移，+1 递进；若生产库存在他人
-- 20261022~20261024 期间未入仓迁移，上线前需补查双库）。
-- tenant_id 一律 10159（千铝工投运行租户）；全部幂等（先查后插 / ON DUPLICATE KEY UPDATE）；
-- 种子 ID 一律按 code 查找、缺失时用 MAX(id)+1 变量分配，不使用固定种子 ID（生产库无法预核
-- ID 占用情况，方案 §5.8 要求）。角色 2000（平台管理员）为一期既有固定 ID，沿用。
-- 新增角色不向任何真实用户绑定 sys_user_role（方案 §5.8/§15）。
-- 挂载点：智能课程材料管理目录（parent_id=2087736644369305601，与一期 textbook_project 同父，
-- 一期菜单 sort_order=5，本批两个菜单取 6/7）。

SET @tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @parent_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND id = 2087736644369305601 AND deleted = 0
  ORDER BY id LIMIT 1
), 2087736644369305601);

-- ============================================================
-- 1) 菜单：我的编写任务 / 我的审定任务（均挂智能课程材料管理目录）
-- ============================================================
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @writing_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_my_writing' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));
SET @review_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_my_review' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@writing_menu_id, @tenant_id, @parent_menu_id, 'menu', 'textbook_my_writing', '我的编写任务',
   '/wo-de-bian-xie', 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookMyTasksPage',
   'textbook:task:view',
   'EditOutlined', 6, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  (@review_menu_id, @tenant_id, @parent_menu_id, 'menu', 'textbook_my_review', '我的审定任务',
   '/wo-de-shen-ding', 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookMyTasksPage',
   'textbook:review:view',
   'AuditOutlined', 7, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @writing_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_my_writing' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @review_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'textbook_my_review' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- ============================================================
-- 2) 权限码：task:view / content:write / review:view / content:review
-- ============================================================
SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
SET @perm_task_view := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:task:view' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @perm_content_write := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:content:write' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @perm_review_view := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:review:view' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));
SET @perm_content_review := COALESCE((
  SELECT id FROM sys_permission
  WHERE tenant_id = @tenant_id AND code = 'textbook:content:review' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_permission_id := @next_permission_id + 1));

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@perm_task_view, @tenant_id, @writing_menu_id,
   'textbook:task:view', '查看我的编写任务', 'view', 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0),
  (@perm_content_write, @tenant_id, @writing_menu_id,
   'textbook:content:write', '编写教材节正文', 'write', 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0),
  (@perm_review_view, @tenant_id, @review_menu_id,
   'textbook:review:view', '查看我的审定任务', 'view', 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0),
  (@perm_content_review, @tenant_id, @review_menu_id,
   'textbook:content:review', '审定教材节正文', 'review', 'enabled', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- ============================================================
-- 3) 业务角色：TEXTBOOK_WRITER / TEXTBOOK_REVIEWER（不绑定任何真实用户）
--    角色不使用固定 ID（按方案 §5.8：固定种子 ID 需双库预核，生产库当前无法访问），
--    先按 code 查找，缺失时 MAX(id)+1 变量分配。
-- ============================================================
SET @next_role_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role);
SET @role_writer_id := COALESCE((
  SELECT id FROM sys_role
  WHERE tenant_id = @tenant_id AND code = 'TEXTBOOK_WRITER' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_role_id := @next_role_id + 1));
SET @role_reviewer_id := COALESCE((
  SELECT id FROM sys_role
  WHERE tenant_id = @tenant_id AND code = 'TEXTBOOK_REVIEWER' AND deleted = 0
  ORDER BY id LIMIT 1
), (@next_role_id := @next_role_id + 1));

INSERT INTO sys_role (id, tenant_id, code, name, role_type, status, remark,
  created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (@role_writer_id, @tenant_id, 'TEXTBOOK_WRITER', '教材编写专家', 'business', 'enabled',
   '教材二期：我的编写任务 + 节正文编写/提交/恢复/图片', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0),
  (@role_reviewer_id, @tenant_id, 'TEXTBOOK_REVIEWER', '教材审定专家', 'business', 'enabled',
   '教材二期：我的审定任务 + 节正文通过/打回/撤销', NOW(), NOW(),
   @seed_operator_id, @seed_operator_id, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), role_type = VALUES(role_type), status = 'enabled',
  remark = VALUES(remark), deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @role_writer_id := (
  SELECT id FROM sys_role
  WHERE tenant_id = @tenant_id AND code = 'TEXTBOOK_WRITER' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @role_reviewer_id := (
  SELECT id FROM sys_role
  WHERE tenant_id = @tenant_id AND code = 'TEXTBOOK_REVIEWER' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- ============================================================
-- 4) 角色授权（三步链第 3 步）：新角色 + 平台管理员（role_id=2000 全功能惯例）
-- ============================================================
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1),
       @tenant_id, t.role_id, t.menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM (
  SELECT @role_writer_id AS role_id, @writing_menu_id AS menu_id FROM DUAL
  UNION ALL SELECT @role_reviewer_id, @review_menu_id FROM DUAL
  UNION ALL SELECT 2000, @writing_menu_id FROM DUAL
  UNION ALL SELECT 2000, @review_menu_id FROM DUAL
) t
WHERE t.role_id IS NOT NULL AND t.menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = t.role_id
      AND existing.menu_id = t.menu_id
      AND existing.deleted = 0
  );

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1),
       @tenant_id, t.role_id, t.permission_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM (
  SELECT @role_writer_id AS role_id, @perm_task_view AS permission_id FROM DUAL
  UNION ALL SELECT @role_writer_id, @perm_content_write FROM DUAL
  UNION ALL SELECT @role_reviewer_id, @perm_review_view FROM DUAL
  UNION ALL SELECT @role_reviewer_id, @perm_content_review FROM DUAL
  UNION ALL SELECT 2000, @perm_task_view FROM DUAL
  UNION ALL SELECT 2000, @perm_content_write FROM DUAL
  UNION ALL SELECT 2000, @perm_review_view FROM DUAL
  UNION ALL SELECT 2000, @perm_content_review FROM DUAL
) t
WHERE t.role_id IS NOT NULL AND t.permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = @tenant_id
      AND existing.role_id = t.role_id
      AND existing.permission_id = t.permission_id
      AND existing.deleted = 0
  );
