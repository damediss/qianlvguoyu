-- 工作台默认材料清单：仅培训照片 + 证书材料为必备/自动项；
-- 申请表、申领花名册、汇总表等改为非必填，由「从模板生成」或替换上传产生。

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
