CREATE TABLE IF NOT EXISTS certification_archive_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  task_id BIGINT NOT NULL,
  task_no VARCHAR(80) NOT NULL,
  task_name VARCHAR(200) NOT NULL,
  batch_name VARCHAR(160) NULL,
  work_type_name VARCHAR(300) NULL,
  level_name VARCHAR(200) NULL,
  completed_at DATETIME NULL,
  candidate_count INT NOT NULL DEFAULT 0,
  archive_status VARCHAR(40) NOT NULL DEFAULT 'NOT_ARCHIVED',
  basic_archived TINYINT(1) NOT NULL DEFAULT 0,
  material_archived TINYINT(1) NOT NULL DEFAULT 0,
  archived_by BIGINT NULL,
  archived_at DATETIME NULL,
  last_operator_id BIGINT NULL,
  last_operator_name VARCHAR(80) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_task (tenant_id, task_id, deleted),
  KEY idx_cert_archive_status (tenant_id, archive_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定归档主表';

CREATE TABLE IF NOT EXISTS certification_archive_basic_snapshot (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  archive_id BIGINT NOT NULL,
  completed_at DATETIME NULL,
  candidate_count INT NOT NULL DEFAULT 0,
  task_json JSON NULL,
  scope_json JSON NULL,
  exam_staff_json JSON NULL,
  device_json JSON NULL,
  room_json JSON NULL,
  evaluator_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_basic (tenant_id, archive_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定归档基础信息快照';

CREATE TABLE IF NOT EXISTS certification_archive_material (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  archive_id BIGINT NOT NULL,
  material_type VARCHAR(80) NOT NULL,
  material_group VARCHAR(80) NOT NULL,
  material_name VARCHAR(200) NOT NULL,
  file_name VARCHAR(300) NOT NULL,
  file_key VARCHAR(1000) NOT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  file_hash VARCHAR(128) NULL,
  content_type VARCHAR(160) NULL,
  file_status VARCHAR(40) NOT NULL DEFAULT 'uploaded',
  uploaded_by BIGINT NULL,
  uploaded_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_material_type (tenant_id, archive_id, material_type, deleted),
  KEY idx_cert_archive_material_archive (tenant_id, archive_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定任务级归档材料';

CREATE TABLE IF NOT EXISTS certification_archive_candidate (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  archive_id BIGINT NOT NULL,
  source_batch_id BIGINT NULL,
  candidate_no VARCHAR(80) NOT NULL,
  name VARCHAR(80) NOT NULL,
  id_card VARCHAR(32) NOT NULL,
  masked_id_card VARCHAR(32) NULL,
  folder_name VARCHAR(300) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_candidate_id_card (tenant_id, archive_id, id_card, deleted),
  KEY idx_cert_archive_candidate_archive (tenant_id, archive_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档考生';

CREATE TABLE IF NOT EXISTS certification_archive_candidate_material (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  archive_candidate_id BIGINT NOT NULL,
  source_batch_id BIGINT NULL,
  material_type VARCHAR(80) NOT NULL,
  material_group VARCHAR(80) NOT NULL,
  file_name VARCHAR(300) NOT NULL,
  file_key VARCHAR(1000) NOT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  file_hash VARCHAR(128) NULL,
  content_type VARCHAR(160) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_candidate_material (tenant_id, archive_candidate_id, material_type, deleted),
  KEY idx_cert_archive_candidate_material_batch (tenant_id, source_batch_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档考生材料';

CREATE TABLE IF NOT EXISTS certification_archive_import_batch (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  archive_id BIGINT NOT NULL,
  batch_no VARCHAR(80) NOT NULL,
  import_type VARCHAR(80) NOT NULL,
  zip_file_name VARCHAR(300) NOT NULL,
  zip_file_key VARCHAR(1000) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'UPLOADED',
  total_candidates INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  error_count INT NOT NULL DEFAULT 0,
  statistics_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_archive_import_batch_no (tenant_id, batch_no, deleted),
  KEY idx_cert_archive_import_archive (tenant_id, archive_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档考生材料导入批次';

CREATE TABLE IF NOT EXISTS certification_archive_import_error (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  batch_id BIGINT NOT NULL,
  candidate_no VARCHAR(80) NULL,
  name VARCHAR(80) NULL,
  id_card VARCHAR(32) NULL,
  error_type VARCHAR(80) NOT NULL,
  error_message VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_archive_import_error_batch (tenant_id, batch_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档导入异常';

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6340, 1, 6300, 'page', 'certification_archive', '认定归档', '/certification/archive', 'certification/Archive', 'certification.archive.task.view', 'FolderOpenOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6990, 1, 6340, 'certification.archive.task.view', '查看认定归档工作台', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6991, 1, 6340, 'certification.archive.record.view', '查看认定归档记录', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6992, 1, 6340, 'certification.archive.basic.view', '查看基础信息归档', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6993, 1, 6340, 'certification.archive.basic.create', '创建基础信息归档', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6994, 1, 6340, 'certification.archive.material.view', '查看认定归档材料', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6995, 1, 6340, 'certification.archive.material.upload', '上传认定归档材料', 'upload', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6996, 1, 6340, 'certification.archive.material.edit', '替换认定归档材料', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6997, 1, 6340, 'certification.archive.material.delete', '删除认定归档材料', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6998, 1, 6340, 'certification.archive.material.download', '下载认定归档材料', 'download', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6999, 1, 6340, 'certification.archive.candidate.view', '查看归档考生材料', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7000, 1, 6340, 'certification.archive.candidate.import', '导入考生归档材料', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7001, 1, 6340, 'certification.archive.import.view', '查看归档导入批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7002, 1, 6340, 'certification.archive.import.confirm', '确认归档导入批次', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7003, 1, 6340, 'certification.archive.import.cancel', '取消归档导入批次', 'cancel', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7004, 1, 6340, 'certification.archive.import.error.export', '导出归档导入异常', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7005, 1, 6340, 'certification.archive.file.preview', '预览归档文件', 'preview', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7006, 1, 6340, 'certification.archive.file.download', '下载归档文件', 'download', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806340, 1, 2000, 6340, NOW(), NOW(), 1, 1, 0),
  (999340, 1, 2604, 6340, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 910000 + p.id - 6990, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.archive.%';

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 910100 + p.id - 6990, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.archive.%';
