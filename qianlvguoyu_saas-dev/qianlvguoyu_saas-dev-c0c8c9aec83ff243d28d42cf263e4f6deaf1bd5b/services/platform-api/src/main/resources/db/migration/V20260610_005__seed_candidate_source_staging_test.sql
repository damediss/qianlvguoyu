-- 测试学员种子数据（用于认定筹备信息收集联调）
INSERT INTO certification_candidate_source_staging (
  id, tenant_id, task_id, source_type, promote_status,
  candidate_name, gender, birth_date, education, graduate_date,
  id_card_no, phone, company_name, company_address, zip_code, company_credit_code,
  apply_work_type_name, cert_work_type_name, work_direction, work_type_code,
  apply_level_name, cert_level_name, apply_work_years, work_resume, training_summary,
  remark, deleted, created_at, updated_at
) VALUES
(
  202606100001, 1, NULL, 'test', 'draft',
  '舒建', '男', '1980-11-05', '硕士研究生', '2016-07-15',
  '522725198011050819', '15078524000',
  '广西百色广投银海铝业有限责任公司', '广西壮族自治区百色市右江区六塘工业园', '533699', '91451000718845643R',
  '铝电解槽操作工', '铝电解工', '铝电解槽操作工', '6-17-06-02',
  '一级/高级技师', '一级/高级技师', '2年',
  '2024年4月-至今，在广西百色广投银海铝业有限责任公司工作',
  '铝电解槽操作工培训已达标准学时',
  '测试学员种子数据-1', 0, NOW(), NOW()
),
(
  202606100002, 1, NULL, 'test', 'draft',
  '占露', '女', '1986-08-05', '普通高中', '2014-07-01',
  '420117198608050469', '18278888593',
  '广西誉升锗业高新技术有限公司', '河池市城东大道南侧锦城国际第1栋第3层', '547000', '91451200MA5KD8K209',
  '浸出工', '化学检验员', '化验员', '6-17-06-02',
  '初级工/五级', '初级工/五级', '3年',
  '2021年08月03日-至今，在誉升锗业任化验员',
  '浸出工培训已达标准学时',
  '测试学员种子数据-2', 0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();
