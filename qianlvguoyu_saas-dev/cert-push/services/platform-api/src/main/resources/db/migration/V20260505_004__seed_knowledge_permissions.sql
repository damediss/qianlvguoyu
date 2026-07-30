INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4600, 1, 0, 'directory', 'knowledge', '知识库', '/knowledge', NULL, 'knowledge:view', 'DatabaseOutlined', 55, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4601, 1, 4600, 'page', 'knowledge_spaces', '企业知识库', '/knowledge/spaces', 'knowledge/KnowledgeSpaceList', 'knowledge:view', NULL, 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4602, 1, 4600, 'page', 'knowledge_search', '知识库检索', '/knowledge/search', 'knowledge/KnowledgeSearch', 'knowledge:search', NULL, 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5110, 1, 4601, 'knowledge:view', '查看知识库', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5111, 1, 4601, 'knowledge:create', '新增知识库', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5112, 1, 4601, 'knowledge:update', '更新知识库', 'update', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5113, 1, 4601, 'knowledge:delete', '删除知识库', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5114, 1, 4601, 'knowledge:document', '知识库资料入库', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5115, 1, 4602, 'knowledge:search', '知识库检索', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5116, 1, 4602, 'knowledge:answer', '知识库问答草稿', 'create', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 600000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE tenant_id = 1 AND id IN (4600, 4601, 4602) AND deleted = 0;

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 700000 + id, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE tenant_id = 1 AND id BETWEEN 5110 AND 5116 AND deleted = 0;
