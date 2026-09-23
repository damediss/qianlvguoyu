-- 培训运营流程「数据面板」菜单文案与排序对齐
UPDATE sys_menu
   SET name = '数据面板',
       status = 'enabled',
       deleted = 0,
       visible = 1,
       sort_order = 1,
       updated_at = NOW()
 WHERE code = 'train_dashboard';
