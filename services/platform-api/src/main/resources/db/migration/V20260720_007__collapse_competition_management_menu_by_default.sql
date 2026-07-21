-- 竞赛管理菜单默认折叠，进入系统时不自动展开。
UPDATE sys_menu
SET expanded = 0,
    updated_at = NOW(),
    updated_by = 1
WHERE deleted = 0
  AND menu_type = 'directory'
  AND code = 'competition_foundation';
