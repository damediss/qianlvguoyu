-- 为培训学员测试数据补充可复现的上游身份证图片来源。
-- 图片为明显标注“仅供系统测试、非有效证件”的虚构测试素材。
UPDATE train_people_list
SET id_card_front = 'classpath:/test-data/certification/test-id-card-front.png',
    id_card_back = 'classpath:/test-data/certification/test-id-card-back.png'
WHERE tenant_id = 1
  AND type = 'student'
  AND id IN (60001, 60301, 60302, 60303, 60304, 60305);

-- 已经导入认定筹备的测试学员同步刷新来源快照，便于直接重新生成 DOCX。
UPDATE certification_candidate_source_staging staging
JOIN train_people_list people
  ON people.tenant_id = staging.tenant_id
 AND people.id = staging.external_ref_id
SET staging.id_card_front_url = people.id_card_front,
    staging.id_card_back_url = people.id_card_back
WHERE staging.source_type = 'training_people'
  AND staging.external_ref_table = 'train_people_list'
  AND staging.deleted = 0
  AND people.id IN (60001, 60301, 60302, 60303, 60304, 60305);
