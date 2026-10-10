-- 修复认定归档权限：按 code 补全缺失项（避免固定 ID 冲突导致 INSERT IGNORE 静默跳过）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710000, 1, 6340, 'certification.archive.task.view', '查看认定归档工作台', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.task.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710001, 1, 6340, 'certification.archive.record.view', '查看认定归档记录', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.record.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710002, 1, 6340, 'certification.archive.basic.view', '查看基础信息归档', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.basic.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710003, 1, 6340, 'certification.archive.basic.create', '创建基础信息归档', 'create', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.basic.create' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710004, 1, 6340, 'certification.archive.material.view', '查看认定归档材料', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.material.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710005, 1, 6340, 'certification.archive.material.upload', '上传认定归档材料', 'upload', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.material.upload' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710006, 1, 6340, 'certification.archive.material.edit', '替换认定归档材料', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.material.edit' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710007, 1, 6340, 'certification.archive.material.delete', '删除认定归档材料', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.material.delete' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710008, 1, 6340, 'certification.archive.material.download', '下载认定归档材料', 'download', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.material.download' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710009, 1, 6340, 'certification.archive.candidate.view', '查看归档考生材料', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.candidate.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710010, 1, 6340, 'certification.archive.candidate.import', '导入考生归档材料', 'import', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.candidate.import' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710011, 1, 6340, 'certification.archive.import.view', '查看归档导入批次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.import.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710012, 1, 6340, 'certification.archive.import.confirm', '确认归档导入批次', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.import.confirm' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710013, 1, 6340, 'certification.archive.import.cancel', '取消归档导入批次', 'cancel', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.import.cancel' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710014, 1, 6340, 'certification.archive.import.error.export', '导出归档导入异常', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.import.error.export' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710015, 1, 6340, 'certification.archive.file.preview', '预览归档文件', 'preview', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.file.preview' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 710016, 1, 6340, 'certification.archive.file.download', '下载归档文件', 'download', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.archive.file.download' AND deleted = 0
);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 920000 + p.id, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.archive.%';

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 920100 + p.id, p.tenant_id, 2604, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code LIKE 'certification.archive.%';
