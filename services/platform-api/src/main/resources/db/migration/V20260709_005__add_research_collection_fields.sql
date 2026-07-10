-- 企业调研人员采集：预报名批次关联 + 精简版模板字段扩展
-- 使用直接 DDL，避免 PREPARE 被 Druid WallFilter 拦截

CREATE TABLE IF NOT EXISTS train_research_org (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL COMMENT '企业/机构名称',
  type VARCHAR(64) NULL COMMENT '类型',
  province_id INT NULL,
  province_name VARCHAR(64) NULL,
  city_id INT NULL,
  city_name VARCHAR(64) NULL,
  area_id INT NULL,
  area_name VARCHAR(64) NULL,
  user_id BIGINT NULL,
  sort_number INT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_research_org_tenant (tenant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训调研企业';

ALTER TABLE train_sign_up_self
  ADD COLUMN research_org_id BIGINT NULL COMMENT '关联企业调研 train_research_org.id' AFTER org_id,
  ADD COLUMN collection_source VARCHAR(32) NOT NULL DEFAULT 'train_info' COMMENT 'train_info=培训信息收集 research=企业调研采集' AFTER research_org_id,
  ADD COLUMN qrcode_token VARCHAR(64) NULL COMMENT '公开采集 token' AFTER collection_source,
  ADD INDEX idx_sign_up_research (research_org_id, deleted);

ALTER TABLE train_sign_up_people_self
  ADD COLUMN department VARCHAR(128) NULL COMMENT '部门' AFTER company_name,
  ADD COLUMN title_major VARCHAR(128) NULL COMMENT '职称专业' AFTER title_type,
  ADD COLUMN title_cert_date VARCHAR(32) NULL COMMENT '职称获证时间' AFTER title_major,
  ADD COLUMN cert_issue_dates TEXT NULL COMMENT '发证日期（多证换行）' AFTER paper_no,
  ADD COLUMN certificates_json TEXT NULL COMMENT '多证结构化 JSON' AFTER cert_issue_dates,
  ADD COLUMN data_source VARCHAR(32) NOT NULL DEFAULT 'h5' COMMENT 'h5/wxmini/import/admin' AFTER certificates_json;

ALTER TABLE train_research_org
  ADD COLUMN target_org_id BIGINT NULL COMMENT '对应 sys_org.id' AFTER title,
  ADD COLUMN active_sign_up_id BIGINT NULL COMMENT '当前进行中的预报名批次' AFTER target_org_id,
  ADD COLUMN personnel_count INT NOT NULL DEFAULT 0 COMMENT '已采集人数' AFTER active_sign_up_id;

ALTER TABLE train_three_year_plan
  ADD COLUMN source_sign_up_id BIGINT NULL COMMENT '人员来源预报名批次' AFTER research_org_id;
