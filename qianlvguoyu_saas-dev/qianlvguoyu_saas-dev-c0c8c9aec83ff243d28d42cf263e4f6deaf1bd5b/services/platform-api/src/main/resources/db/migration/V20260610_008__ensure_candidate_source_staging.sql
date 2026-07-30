-- 补救迁移：库中 flyway 版本已高于 004~006，此前建表/种子/权限脚本被跳过。
-- 本脚本幂等执行，确保 staging 表、测试数据与生成权限存在。

CREATE TABLE IF NOT EXISTS certification_candidate_source_staging (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NULL COMMENT '目标认定任务id（导入前可空）',
  candidate_id BIGINT NULL COMMENT '已晋升的认定考生id',
  source_type VARCHAR(40) NOT NULL DEFAULT 'test' COMMENT '来源类型: test|excel|training_sign_up|training_people|manual',
  external_ref_table VARCHAR(80) NULL COMMENT '外部来源表名，如 train_sign_up_people_self',
  external_ref_id BIGINT NULL COMMENT '外部来源主键',
  sign_up_id BIGINT NULL COMMENT '培训报名计划id（未来对接 Excel:sign_up_id）',
  promote_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT 'draft|promoted|failed',
  promote_message VARCHAR(500) NULL COMMENT '晋升失败原因',
  promoted_at DATETIME NULL COMMENT '晋升认定考生时间',
  candidate_name VARCHAR(80) NOT NULL COMMENT '姓名',
  gender VARCHAR(10) NULL COMMENT '性别',
  birth_date DATE NULL COMMENT '出生年月',
  education VARCHAR(50) NULL COMMENT '文化程度',
  graduate_date DATE NULL COMMENT '毕业时间',
  school VARCHAR(200) NULL COMMENT '毕业院校',
  major VARCHAR(100) NULL COMMENT '专业',
  nation VARCHAR(20) NULL COMMENT '民族',
  id_card_no VARCHAR(32) NOT NULL COMMENT '身份证号',
  phone VARCHAR(32) NULL COMMENT '电话',
  home_address VARCHAR(500) NULL COMMENT '个人住址',
  company_name VARCHAR(200) NULL COMMENT '工作单位',
  company_address VARCHAR(500) NULL COMMENT '单位地址',
  zip_code VARCHAR(20) NULL COMMENT '邮编',
  company_credit_code VARCHAR(32) NULL COMMENT '统一社会信用代码',
  industrial_park VARCHAR(200) NULL COMMENT '所在产业园',
  work_start_date DATE NULL COMMENT '参加工作时间',
  original_work_type_name VARCHAR(120) NULL COMMENT '原职业工种',
  original_level_name VARCHAR(80) NULL COMMENT '原技术等级',
  original_cert_no VARCHAR(80) NULL COMMENT '原证书编号',
  apply_work_type_id BIGINT NULL COMMENT '申报工种id',
  apply_work_type_name VARCHAR(120) NULL COMMENT '申报职业工种',
  cert_work_type_id BIGINT NULL COMMENT '认定工种id',
  cert_work_type_name VARCHAR(120) NULL COMMENT '认定职业',
  work_direction VARCHAR(120) NULL COMMENT '工种方向',
  work_type_code VARCHAR(40) NULL COMMENT '职业代码',
  apply_level_code VARCHAR(40) NULL COMMENT '申报等级编码',
  apply_level_name VARCHAR(80) NULL COMMENT '申报等级',
  cert_level_name VARCHAR(80) NULL COMMENT '认定等级',
  apply_work_years VARCHAR(20) NULL COMMENT '申报工种工龄',
  apply_condition VARCHAR(500) NULL COMMENT '申报条件',
  work_resume TEXT NULL COMMENT '个人工作简历',
  theory_score DECIMAL(10, 2) NULL COMMENT '理论成绩',
  practical_score DECIMAL(10, 2) NULL COMMENT '实操成绩',
  training_summary VARCHAR(1000) NULL COMMENT '参加培训情况',
  photo_url VARCHAR(500) NULL COMMENT '证件照',
  photo_alt_url VARCHAR(500) NULL COMMENT '证件照备用',
  id_card_front_url VARCHAR(500) NULL COMMENT '身份证正面',
  id_card_back_url VARCHAR(500) NULL COMMENT '身份证反面',
  original_cert_image_url VARCHAR(500) NULL COMMENT '原工种证书',
  education_cert_image_url VARCHAR(500) NULL COMMENT '学历证明',
  platform_user_id BIGINT NULL COMMENT '平台用户id',
  user_id BIGINT NULL COMMENT '用户id',
  raw_payload JSON NULL COMMENT '外部原始行JSON',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_candidate_staging_task (tenant_id, task_id, promote_status, deleted),
  KEY idx_cert_candidate_staging_source (tenant_id, source_type, external_ref_table, external_ref_id, deleted),
  KEY idx_cert_candidate_staging_id_card (tenant_id, task_id, id_card_no, deleted),
  KEY idx_cert_candidate_staging_candidate (tenant_id, candidate_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定考生来源暂存（Word映射测试/培训对接缓冲）';

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

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202606100301, 1, 6310, 'certification.preparation.document.generate', '生成报名信息表', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.preparation.document.generate' AND deleted = 0
);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT 202606100401 + r.id, r.id, 202606100301, 1, NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.code IN ('platform_admin', 'super_admin')
  AND EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = 202606100301 AND p.deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 202606100301 AND rp.deleted = 0
  );
