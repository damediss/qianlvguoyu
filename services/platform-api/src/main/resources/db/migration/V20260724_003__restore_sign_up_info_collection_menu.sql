-- 恢复独立「信息收集」菜单可见（保留开班准备按班收集能力）
UPDATE sys_menu
SET visible = 1,
    updated_at = NOW()
WHERE code IN ('train_sign-up', 'train_sign-up-people')
  AND deleted = 0;
