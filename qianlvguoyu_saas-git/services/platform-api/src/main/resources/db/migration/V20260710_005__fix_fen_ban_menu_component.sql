-- 对齐 fen_ban 菜单 component 字段（前端 pageMap 已映射 TrainingClassPeoplePage）

UPDATE sys_menu
SET
  component = 'training/TrainingClassDivisionPage',
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'fen_ban'
  AND deleted = 0;
