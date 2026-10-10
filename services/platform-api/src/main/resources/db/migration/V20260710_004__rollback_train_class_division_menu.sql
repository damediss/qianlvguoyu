-- 回滚 V20260710_002 / V20260710_003：撤销培训运营流程「分班」菜单调整

-- 1. train_needs 恢复为培训管理下的一级菜单
UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'training'
 AND parent.deleted = 0
SET
  child.parent_id = parent.id,
  child.name = '培训需求',
  child.component = 'training/TrainingNeeds',
  child.sort_order = 1,
  child.updated_at = NOW(),
  child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'train_needs'
  AND child.deleted = 0;

-- 2. train_class-list 恢复为隐藏状态（变更前即为软删除）
UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'training'
 AND parent.deleted = 0
SET
  child.parent_id = parent.id,
  child.name = '分班管理',
  child.component = 'training/TrainingClassList',
  child.sort_order = 12,
  child.visible = 0,
  child.deleted = 1,
  child.status = 'enabled',
  child.updated_at = NOW(),
  child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'train_class-list';

-- 3. 企业「需求管理」目录恢复挂到培训运营流程下（变更前位置）
UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'pxyun_ying_liu_cheng'
 AND parent.deleted = 0
SET
  child.parent_id = parent.id,
  child.sort_order = 1,
  child.updated_at = NOW(),
  child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'company_demand'
  AND child.deleted = 0;

-- 4. 移除误建的 training_operation_flow 目录
UPDATE sys_menu
SET
  visible = 0,
  deleted = 1,
  status = 'disabled',
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'training_operation_flow';

-- 5. 撤销为非平台管理员新增的「分班」角色菜单（保留 PLATFORM_ADMIN 原有授权）
UPDATE sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id
INNER JOIN sys_role r ON r.id = rm.role_id AND r.tenant_id = rm.tenant_id
SET
  rm.deleted = 1,
  rm.updated_at = NOW(),
  rm.updated_by = 1
WHERE rm.deleted = 0
  AND m.code = 'train_class-list'
  AND UPPER(r.code) NOT IN ('PLATFORM_ADMIN', 'SUPER_ADMIN');
