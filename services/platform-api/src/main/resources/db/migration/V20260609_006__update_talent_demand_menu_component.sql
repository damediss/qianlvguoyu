-- 人才需求菜单组件指向 React 页面
UPDATE sys_menu
SET component = 'training/EnterpriseTalentDemandPage',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_demand-employee'
  AND deleted = 0;
