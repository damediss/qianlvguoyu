-- 将竞赛计划列表菜单名称统一为「计划管理」。
-- 使用菜单编码定位，保证重复执行幂等且不影响权限关系。
UPDATE sys_menu
SET name = '计划管理',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'competition_event'
  AND deleted = 0;
