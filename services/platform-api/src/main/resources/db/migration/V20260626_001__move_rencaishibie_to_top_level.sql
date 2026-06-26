-- Move rencaishibie from knowledge sub-menu to top-level (same level as knowledge)
-- Before: 知识库 → 人才识别 (parent_id=4600, page)
-- After:  人才识别 (parent_id=0, page, sort_order=56, right after 知识库 sort_order=55)

UPDATE sys_menu
SET parent_id = 0,
    path = '/rencaishibie',
    component = 'knowledge/RencaishibieEmbed',
    sort_order = 56,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'knowledge_rencaishibie'
  AND deleted = 0;