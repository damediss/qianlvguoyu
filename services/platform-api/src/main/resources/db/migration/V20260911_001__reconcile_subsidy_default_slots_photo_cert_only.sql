-- 补偿迁移：统一测试库与生产库对 20260909.003 的版本分叉。
-- 已应用过的数据库重复执行安全；未应用的数据库补齐默认材料槽位配置。

UPDATE train_subsidy_pack_slot
SET required_flag = 0,
    remark = CASE
      WHEN remark IS NULL OR remark = '' THEN '非默认清单：需从模板生成或手工上传'
      WHEN remark LIKE '%非默认清单%' THEN remark
      ELSE CONCAT(remark, '；非默认清单：需从模板生成或手工上传')
    END
WHERE deleted = 0
  AND slot_code NOT IN (
    'photo_album',
    'cert_pass',
    'cert_retake',
    'cert_pdf_bundle',
    'cert_roster'
  )
  AND renderer <> 'follow_photo';

UPDATE train_subsidy_pack_slot
SET required_flag = 1
WHERE deleted = 0
  AND (
    slot_code IN ('photo_album', 'cert_pdf_bundle')
    OR renderer = 'follow_photo'
  );
