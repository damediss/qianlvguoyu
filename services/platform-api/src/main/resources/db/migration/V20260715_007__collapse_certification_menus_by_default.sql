-- 认定管理及子目录默认折叠（expanded=0），进入系统时不自动展开
UPDATE sys_menu
SET expanded = 0,
    updated_at = NOW()
WHERE deleted = 0
  AND menu_type = 'directory'
  AND (code = 'certification' OR code LIKE 'certification_%');
