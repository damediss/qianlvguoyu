-- 认定考生来源暂存表（测试/对接缓冲）
-- 用途：在培训部 train_sign_up_people_self 未就绪前，由认定模块自行维护完整字段并完成 Word 映射验证。
-- 后续培训表就绪后，通过 source_type + external_ref 接入，本表结构保持不变。
--
-- 数据流：
--   [test 手工/Excel] ──┐
--                       ├──> certification_candidate_source_staging ──> certification_candidate (+ Word DOC001)
--   [training_sign_up] ─┘      (未来：external_ref_table='train_sign_up_people_self')

CREATE TABLE IF NOT EXISTS certification_candidate_source_staging (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NULL COMMENT '目标认定任务id（导入前可空）',
  candidate_id BIGINT NULL COMMENT '已晋升的认定考生id',

  -- 来源溯源（解耦培训部表，不做跨库外键）
  source_type VARCHAR(40) NOT NULL DEFAULT 'test' COMMENT '来源类型: test|excel|training_sign_up|training_people|manual',
  external_ref_table VARCHAR(80) NULL COMMENT '外部来源表名，如 train_sign_up_people_self',
  external_ref_id BIGINT NULL COMMENT '外部来源主键',
  sign_up_id BIGINT NULL COMMENT '培训报名计划id（未来对接 Excel:sign_up_id）',

  promote_status VARCHAR(40) NOT NULL DEFAULT 'draft' COMMENT 'draft|promoted|failed',
  promote_message VARCHAR(500) NULL COMMENT '晋升失败原因',
  promoted_at DATETIME NULL COMMENT '晋升认定考生时间',

  -- === A. 个人基本信息（Word 占位符见 COMMENT） ===
  candidate_name VARCHAR(80) NOT NULL COMMENT '姓名 | {{candidate_name}} | Excel:name | cert.candidate_name',
  gender VARCHAR(10) NULL COMMENT '性别 | {{gender}} | 推导:id_card_no | cert.[扩展]',
  birth_date DATE NULL COMMENT '出生年月 | {{birth_date}} | 推导:id_card_no | cert.[扩展]',
  education VARCHAR(50) NULL COMMENT '文化程度 | {{education}} | Excel:education',
  graduate_date DATE NULL COMMENT '毕业时间 | {{graduate_date}} | Excel:graduate_date',
  school VARCHAR(200) NULL COMMENT '毕业院校 | {{school}} | Excel:school',
  major VARCHAR(100) NULL COMMENT '专业 | {{major}} | Excel:major',
  nation VARCHAR(20) NULL COMMENT '民族 | {{nation}} | Excel:nation',
  id_card_no VARCHAR(32) NOT NULL COMMENT '身份证号 | {{id_card_no}} | Excel:id_card_number | cert.id_card_no',
  phone VARCHAR(32) NULL COMMENT '电话 | {{phone}} | Excel:phone | cert.phone',
  home_address VARCHAR(500) NULL COMMENT '个人住址 | {{home_address}} | Excel:address',

  -- === B. 单位信息 ===
  company_name VARCHAR(200) NULL COMMENT '工作单位 | {{company_name}} | Excel:company_name | cert.org_name',
  company_address VARCHAR(500) NULL COMMENT '单位地址 | {{company_address}} | Excel:company_address',
  zip_code VARCHAR(20) NULL COMMENT '邮编 | {{zip_code}} | Excel:zip_code',
  company_credit_code VARCHAR(32) NULL COMMENT '统一社会信用代码 | {{company_code}} | Excel:company_code',
  industrial_park VARCHAR(200) NULL COMMENT '所在产业园 | {{industrial_park}} | Excel:industrial_park',

  -- === C. 职业与等级 ===
  work_start_date DATE NULL COMMENT '参加工作时间 | {{work_start_date}} | Excel:work_date_start',
  original_work_type_name VARCHAR(120) NULL COMMENT '原职业工种 | {{original_work_type}} | Excel:work_type_paper_name|job',
  original_level_name VARCHAR(80) NULL COMMENT '原技术等级 | {{original_level}} | Excel:work_type_paper_level',
  original_cert_no VARCHAR(80) NULL COMMENT '原证书编号 | {{original_cert_no}} | Excel:work_type_paper_no|paper_no',
  apply_work_type_id BIGINT NULL COMMENT '申报工种id | lookup | Excel:req_work_type_id | cert.work_type_id',
  apply_work_type_name VARCHAR(120) NULL COMMENT '申报职业工种 | {{apply_work_type}} | Excel:req_work_type_name | cert.work_type_name',
  cert_work_type_id BIGINT NULL COMMENT '认定工种id | lookup | Excel:work_type_id',
  cert_work_type_name VARCHAR(120) NULL COMMENT '认定职业 | {{cert_work_type}} | Excel:work_type_name',
  work_direction VARCHAR(120) NULL COMMENT '工种方向 | {{work_direction}} | Excel:job',
  work_type_code VARCHAR(40) NULL COMMENT '职业代码 | {{work_type_code}} | lookup:exam_work_type.series_no',
  apply_level_code VARCHAR(40) NULL COMMENT '申报等级编码 | Excel:req_level | cert.level_code',
  apply_level_name VARCHAR(80) NULL COMMENT '申报等级 | {{apply_level}} | Excel:level | cert.level_name',
  cert_level_name VARCHAR(80) NULL COMMENT '认定等级 | {{cert_level}} | Excel:level',
  apply_work_years VARCHAR(20) NULL COMMENT '申报工种工龄 | {{apply_work_years}} | Excel:work_time|work_exp',
  apply_condition VARCHAR(500) NULL COMMENT '申报条件 | {{apply_condition}} | Excel:condition',
  work_resume TEXT NULL COMMENT '个人工作简历 | {{work_resume}} | Excel:work_exp(JSON格式化)',

  -- === D. 成绩（认定后回填，测试阶段可手工填） ===
  theory_score DECIMAL(10, 2) NULL COMMENT '理论成绩 | {{theory_score}}',
  practical_score DECIMAL(10, 2) NULL COMMENT '实操成绩 | {{practical_score}}',

  -- === E. 培训情况（未来由培训模块同步） ===
  training_summary VARCHAR(1000) NULL COMMENT '参加培训情况 | {{training_summary}} | Excel:task|sign_up_id',

  -- === F. 附件 URL（Word 嵌入 / 转存 file_id） ===
  photo_url VARCHAR(500) NULL COMMENT '证件照 | {{photo}} | Excel:id_card_header',
  photo_alt_url VARCHAR(500) NULL COMMENT '证件照备用 | {{photo_alt}} | Excel:id_card_header2',
  id_card_front_url VARCHAR(500) NULL COMMENT '身份证正面 | {{id_card_front}} | Excel:id_card_front',
  id_card_back_url VARCHAR(500) NULL COMMENT '身份证反面 | {{id_card_back}} | Excel:id_card_back',
  original_cert_image_url VARCHAR(500) NULL COMMENT '原工种证书 | {{original_cert_image}} | Excel:work_type_paper',
  education_cert_image_url VARCHAR(500) NULL COMMENT '学历证明 | {{education_cert_image}} | Excel:school_paper',

  -- === G. 扩展溯源（培训表就绪后原样透传，认定侧不解析） ===
  platform_user_id BIGINT NULL COMMENT '平台用户id | Excel:platform_user_id',
  user_id BIGINT NULL COMMENT '用户id | Excel:user_id',
  raw_payload JSON NULL COMMENT '外部原始行JSON（完整保留培训部字段）',

  remark VARCHAR(500) NULL COMMENT '备注 | {{remark}} | Excel:comments | cert.remark',
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
