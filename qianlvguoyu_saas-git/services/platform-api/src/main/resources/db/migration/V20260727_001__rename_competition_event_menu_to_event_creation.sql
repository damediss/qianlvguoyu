-- 将竞赛赛事入口名称统一为「赛事创建」。
-- 使用菜单编码定位，保证重复执行幂等且不影响权限关系。
UPDATE sys_menu
SET name = '赛事创建',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'competition_event'
  AND deleted = 0;
