-- V20260813_004: 补贴台账强化 + 材料配方 Profile/Slot（《申报补贴材料生成实现方案》S1/P1）
-- 注：20260813.002/003 已被 standard_mubiao 占用，故本脚本用 004。

-- ---------- train_subsidy 增量 ----------
ALTER TABLE train_subsidy
  ADD COLUMN class_id BIGINT NULL COMMENT '班级 train_info_class.id（首期强制）' AFTER project_id,
  ADD COLUMN train_info_id BIGINT NULL COMMENT '冗余培训单' AFTER class_id,
  ADD COLUMN phase VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/checking/ready/submitted/rejected/approved/granting/granted/closed' AFTER status,
  ADD COLUMN region_code VARCHAR(64) NULL COMMENT '申报地市编码/名' AFTER phase,
  ADD COLUMN region_name VARCHAR(128) NULL COMMENT '申报地市展示名' AFTER region_code,
  ADD COLUMN train_mode VARCHAR(32) NULL COMMENT 'skill_upgrade/apprentice' AFTER region_name,
  ADD COLUMN policy_year INT NULL COMMENT '政策年' AFTER train_mode,
  ADD COLUMN profile_id BIGINT NULL COMMENT '锁定配方' AFTER policy_year,
  ADD COLUMN profile_version VARCHAR(32) NULL COMMENT '锁定配方版本' AFTER profile_id,
  ADD COLUMN profile_locked TINYINT NOT NULL DEFAULT 0 COMMENT '1=已锁定配方' AFTER profile_version,
  ADD COLUMN checklist_json MEDIUMTEXT NULL COMMENT '齐套快照 JSON' AFTER profile_locked,
  ADD COLUMN evidence_index_path VARCHAR(512) NULL COMMENT '证据索引包路径' AFTER checklist_json;

ALTER TABLE train_subsidy
  ADD KEY idx_ts_class (class_id),
  ADD KEY idx_ts_phase (phase),
  ADD KEY idx_ts_region_mode_year (region_code, train_mode, policy_year);

UPDATE train_subsidy SET phase = CASE
  WHEN status = 1 THEN 'submitted'
  WHEN status = 2 THEN 'approved'
  ELSE 'draft'
END
WHERE phase = 'draft' OR phase IS NULL OR phase = '';

-- ---------- 事件时间线 ----------
CREATE TABLE IF NOT EXISTS train_subsidy_event (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  subsidy_id    BIGINT       NOT NULL,
  action        VARCHAR(64)  NOT NULL COMMENT 'create/update/transition/generate/upload_slot/...',
  from_phase    VARCHAR(32)  NULL,
  to_phase      VARCHAR(32)  NULL,
  remark        VARCHAR(1000) NULL,
  payload_json  MEDIUMTEXT   NULL,
  operator_id   BIGINT       NULL,
  tenant_id     BIGINT       NOT NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tse_subsidy (subsidy_id, deleted),
  KEY idx_tse_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴进度事件';

-- ---------- 材料配方 ----------
CREATE TABLE IF NOT EXISTS train_subsidy_pack_profile (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  region_code     VARCHAR(64)  NOT NULL,
  region_name     VARCHAR(128) NOT NULL,
  train_mode      VARCHAR(32)  NOT NULL COMMENT 'skill_upgrade/apprentice',
  policy_year     INT          NOT NULL,
  profile_version VARCHAR(32)  NOT NULL DEFAULT 'v1',
  title           VARCHAR(200) NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/active/retired',
  is_default      TINYINT      NOT NULL DEFAULT 0 COMMENT '同年同地市同模式默认',
  remark          VARCHAR(500) NULL,
  tenant_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '0=平台级共享配方',
  deleted         TINYINT      NOT NULL DEFAULT 0,
  created_by      BIGINT       NULL,
  updated_by      BIGINT       NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tspp_key (tenant_id, region_code, train_mode, policy_year, profile_version, deleted),
  KEY idx_tspp_active (region_code, train_mode, policy_year, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴材料配方';

CREATE TABLE IF NOT EXISTS train_subsidy_pack_slot (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  profile_id      BIGINT       NOT NULL,
  slot_code       VARCHAR(64)  NOT NULL,
  title           VARCHAR(200) NOT NULL,
  required_flag   TINYINT      NOT NULL DEFAULT 1,
  sort_order      INT          NOT NULL DEFAULT 0,
  renderer        VARCHAR(32)  NOT NULL COMMENT 'docx_var/xlsx_row/file_copy/pdf_merge/manual_upload/follow_photo',
  template_id     BIGINT       NULL,
  data_binding    VARCHAR(500) NULL,
  folder_path     VARCHAR(200) NULL COMMENT 'ZIP 内相对目录',
  remark          VARCHAR(500) NULL,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tsps_profile (profile_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴材料配方槽位';

-- 人工上传占位（生成前可挂文件）
CREATE TABLE IF NOT EXISTS train_subsidy_slot_file (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  subsidy_id    BIGINT       NOT NULL,
  slot_code     VARCHAR(64)  NOT NULL,
  file_id       BIGINT       NULL,
  file_path     VARCHAR(512) NOT NULL,
  file_name     VARCHAR(255) NULL,
  uploaded_by   BIGINT       NULL,
  tenant_id     BIGINT       NOT NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tssf_sub_slot (subsidy_id, slot_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴单槽位人工附件';

-- ---------- 种子：来宾 × skill_upgrade × 2024 v1 ----------
INSERT INTO train_subsidy_pack_profile
  (id, region_code, region_name, train_mode, policy_year, profile_version, title, status, is_default, remark, tenant_id, deleted)
VALUES
  (1, '来宾', '来宾市', 'skill_upgrade', 2024, 'v1', '来宾岗位技能提升2024', 'active', 1, 'P2试点配方', 0, 0),
  (2, '来宾', '来宾市', 'skill_upgrade', 2025, 'v1', '来宾岗位技能提升2025（空壳可复制）', 'draft', 1, '新年空壳', 0, 0),
  (3, '玉林', '玉林市', 'skill_upgrade', 2024, 'v1', '玉林岗位技能提升2024', 'active', 1, NULL, 0, 0),
  (4, '河池南丹', '河池市南丹县', 'skill_upgrade', 2024, 'v1', '河池南丹岗位技能提升2024', 'active', 1, NULL, 0, 0);

INSERT INTO train_subsidy_pack_slot
  (profile_id, slot_code, title, required_flag, sort_order, renderer, folder_path, remark)
VALUES
  (1, 'cover_toc', '封面/申请目录', 1, 10, 'manual_upload', '01_封面/', '首期人工上传'),
  (1, 'fund_request', '拨付请示/资金申请', 1, 20, 'manual_upload', '02_拨付请示/', NULL),
  (1, 'apply_form', '补贴申请表', 1, 30, 'manual_upload', '03_申请表/', NULL),
  (1, 'claim_roster', '申领培训补贴人员花名册', 1, 40, 'manual_upload', '04_花名册/', NULL),
  (1, 'cert_pass', '正考获证名册', 0, 50, 'manual_upload', '05_获证/', NULL),
  (1, 'cert_retake', '补考获证名册', 0, 55, 'manual_upload', '05_获证/', NULL),
  (1, 'summary_sheet', '补贴汇总表', 1, 60, 'manual_upload', '06_汇总/', NULL),
  (1, 'syllabus', '教学大纲', 0, 70, 'manual_upload', '07_大纲/', NULL),
  (1, 'photo_album', '培训照片', 1, 80, 'follow_photo', '08_培训照片/', '优先跟班照片册'),
  (1, 'cert_pdf_bundle', '证书PDF', 1, 90, 'manual_upload', '09_证书/', '首期批量上传'),
  (3, 'proxy_agreement', '代为申请培训补贴协议', 1, 10, 'manual_upload', '01_协议/', NULL),
  (3, 'satisfaction', '学员满意度调查表', 1, 20, 'manual_upload', '02_满意度/', NULL),
  (3, 'train_plan', '培训方案', 1, 30, 'manual_upload', '03_方案/', NULL),
  (3, 'fund_request', '拨付请示', 1, 40, 'manual_upload', '04_拨付/', NULL),
  (3, 'claim_roster', '申领花名册', 1, 50, 'manual_upload', '05_花名册/', NULL),
  (3, 'photo_album', '培训照片', 1, 60, 'follow_photo', '06_培训照片/', NULL),
  (3, 'cert_pdf_bundle', '证书PDF', 0, 70, 'manual_upload', '07_证书/', NULL),
  (4, 'fund_request', '拨付请示', 1, 10, 'manual_upload', '01_拨付/', NULL),
  (4, 'contract_sign', '合同签订情况表', 1, 20, 'manual_upload', '02_合同/', NULL),
  (4, 'claim_roster', '申领花名册', 1, 30, 'manual_upload', '03_花名册/', NULL),
  (4, 'cert_roster', '获证花名册', 1, 40, 'manual_upload', '04_获证/', NULL),
  (4, 'photo_album', '培训照片', 1, 50, 'follow_photo', '05_培训照片/', NULL),
  (4, 'cert_pdf_bundle', '证书PDF合并', 1, 60, 'manual_upload', '06_证书/', NULL);
