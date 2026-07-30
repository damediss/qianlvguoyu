-- 考务信息仅保留姓名与联系电话，其余业务字段下线
ALTER TABLE certification_info_exam_staff
  DROP INDEX uk_cert_info_exam_staff_cert;

ALTER TABLE certification_info_exam_staff
  DROP COLUMN user_id,
  DROP COLUMN certificate_no,
  DROP COLUMN work_type_scope,
  DROP COLUMN work_type_json,
  DROP COLUMN level_scope,
  DROP COLUMN level_json,
  DROP COLUMN valid_until,
  DROP COLUMN org_id,
  DROP COLUMN org_name,
  DROP COLUMN materials_description,
  DROP COLUMN remark;
