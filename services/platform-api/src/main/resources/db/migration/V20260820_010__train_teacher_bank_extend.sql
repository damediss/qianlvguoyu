-- 师资库扩展：Excel/迁移字段 + 证照附件表
-- 版本号避开已占用的 20260820.001（create_practical_complete_sign）
ALTER TABLE train_teacher_bank
  ADD COLUMN skill_name VARCHAR(512) NULL COMMENT '职业技能名称' AFTER major,
  ADD COLUMN qualification_level VARCHAR(255) NULL COMMENT '职业资格等级' AFTER skill_name,
  ADD COLUMN professional_title VARCHAR(255) NULL COMMENT '专业技术职务' AFTER qualification_level,
  ADD COLUMN cert_text TEXT NULL COMMENT '资格证书原文' AFTER professional_title,
  ADD COLUMN teacher_qual_no VARCHAR(128) NULL COMMENT '教师资格证号' AFTER cert_text,
  ADD COLUMN title_cert_no VARCHAR(128) NULL COMMENT '职称证号' AFTER teacher_qual_no,
  ADD COLUMN skill_cert_no VARCHAR(255) NULL COMMENT '技能等级证号摘要' AFTER title_cert_no,
  ADD COLUMN id_card_front_file_id BIGINT NULL COMMENT '身份证正面文件' AFTER skill_cert_no,
  ADD COLUMN id_card_back_file_id BIGINT NULL COMMENT '身份证反面文件' AFTER id_card_front_file_id,
  ADD COLUMN graduate_file_id BIGINT NULL COMMENT '毕业证文件' AFTER id_card_back_file_id,
  ADD COLUMN education_file_id BIGINT NULL COMMENT '学历证文件' AFTER graduate_file_id,
  ADD COLUMN teacher_qual_file_id BIGINT NULL COMMENT '教师资格证文件' AFTER education_file_id,
  ADD COLUMN title_cert_file_id BIGINT NULL COMMENT '职称证文件' AFTER teacher_qual_file_id,
  ADD COLUMN skill_cert_file_id BIGINT NULL COMMENT '技能等级证文件' AFTER title_cert_file_id,
  ADD COLUMN legacy_id BIGINT NULL COMMENT 'PHP师资库原id' AFTER skill_cert_file_id,
  ADD COLUMN source VARCHAR(32) NULL COMMENT 'legacy/excel/manual' AFTER legacy_id;

CREATE UNIQUE INDEX uk_teacher_bank_legacy ON train_teacher_bank (tenant_id, legacy_id);

CREATE TABLE IF NOT EXISTS train_teacher_bank_file (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  file_type VARCHAR(32) NOT NULL COMMENT 'ID_FRONT/ID_BACK/GRADUATE/EDUCATION/TEACHER_QUAL/TITLE/SKILL/OTHER',
  file_id BIGINT NOT NULL,
  legacy_url VARCHAR(1024) NULL,
  cert_no VARCHAR(128) NULL,
  sort_no INT NOT NULL DEFAULT 0,
  remark VARCHAR(255) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_teacher_bank_file_teacher (teacher_id, deleted),
  KEY idx_teacher_bank_file_tenant (tenant_id, teacher_id),
  KEY idx_teacher_bank_file_legacy (tenant_id, deleted)
) COMMENT='师资库证照附件';
