-- V20260610_004: 组卷计划 + 知识比重 菜单/权限种子数据
-- 组卷计划菜单已存在(id=2064515500600889345, code=zu_juan_ji_hia)，不重复插入

-- 1. 知识比重菜单 (挂在"提升考试管理"下)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(6501, 1, 2064170197574156289, 'page', 'exam_knowledge_rate', '知识比重',
 '/question-bank/knowledge-rate', 'exam/KnowledgeRate', 'exam:knowledge-rate:view',
 'PieChartOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2. 组卷计划权限 (8条: 7200-7207)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
(7200, 1, 2064515500600889345, 'exam:make-plan:view',   '查看组卷计划',   'view',   'enabled', NOW(), NOW(), 1, 1, 0),
(7201, 1, 2064515500600889345, 'exam:make-plan:create', '创建组卷计划',   'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(7202, 1, 2064515500600889345, 'exam:make-plan:update', '修改组卷计划',   'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(7203, 1, 2064515500600889345, 'exam:make-plan:delete', '删除组卷计划',   'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
(7204, 1, 2064515500600889345, 'exam:paper:view',       '查看试卷',       'view',   'enabled', NOW(), NOW(), 1, 1, 0),
(7205, 1, 2064515500600889345, 'exam:paper:create',     '生成试卷',       'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(7206, 1, 2064515500600889345, 'exam:paper:update',     '修改试卷',       'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(7207, 1, 2064515500600889345, 'exam:paper:delete',     '删除试卷',       'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 知识比重权限 (4条: 7210-7213)
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
(7210, 1, 6501, 'exam:knowledge-rate:view',   '查看知识比重',   'view',   'enabled', NOW(), NOW(), 1, 1, 0),
(7211, 1, 6501, 'exam:knowledge-rate:create', '创建知识比重',   'create', 'enabled', NOW(), NOW(), 1, 1, 0),
(7212, 1, 6501, 'exam:knowledge-rate:update', '修改知识比重',   'update', 'enabled', NOW(), NOW(), 1, 1, 0),
(7213, 1, 6501, 'exam:knowledge-rate:delete', '删除知识比重',   'delete', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4. PLATFORM_ADMIN (role_id=2000) 菜单关联
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(930000, 1, 2000, 2064515500600889345, NOW(), NOW(), 1, 1, 0),
(930001, 1, 2000, 6501, NOW(), NOW(), 1, 1, 0);

-- 5. PLATFORM_ADMIN (role_id=2000) 权限关联
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(930100, 1, 2000, 7200, NOW(), NOW(), 1, 1, 0),
(930101, 1, 2000, 7201, NOW(), NOW(), 1, 1, 0),
(930102, 1, 2000, 7202, NOW(), NOW(), 1, 1, 0),
(930103, 1, 2000, 7203, NOW(), NOW(), 1, 1, 0),
(930104, 1, 2000, 7204, NOW(), NOW(), 1, 1, 0),
(930105, 1, 2000, 7205, NOW(), NOW(), 1, 1, 0),
(930106, 1, 2000, 7206, NOW(), NOW(), 1, 1, 0),
(930107, 1, 2000, 7207, NOW(), NOW(), 1, 1, 0),
(930110, 1, 2000, 7210, NOW(), NOW(), 1, 1, 0),
(930111, 1, 2000, 7211, NOW(), NOW(), 1, 1, 0),
(930112, 1, 2000, 7212, NOW(), NOW(), 1, 1, 0),
(930113, 1, 2000, 7213, NOW(), NOW(), 1, 1, 0);
