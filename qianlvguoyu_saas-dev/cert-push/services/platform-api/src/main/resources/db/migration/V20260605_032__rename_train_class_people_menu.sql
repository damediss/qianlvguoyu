-- 对齐 yun_shang classMembers.vue：侧边栏显示「班级人员」，隐藏重复的「培训人员」菜单

UPDATE sys_menu
SET name = '班级人员',
    path = '/training/class-members',
    component = 'training/TrainingClassPeoplePage',
    updated_at = NOW(),
    updated_by = 1
WHERE code = 'train_people-list'
  AND deleted = 0;

-- 旧菜单 train_people（培训人员）与 train_people-list 功能重复，隐藏避免混淆
UPDATE sys_menu
SET visible = 0,
    updated_at = NOW(),
    updated_by = 1
WHERE code = 'train_people'
  AND deleted = 0;
