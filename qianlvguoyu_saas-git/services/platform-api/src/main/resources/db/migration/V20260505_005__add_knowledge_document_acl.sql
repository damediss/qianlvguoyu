ALTER TABLE kb_document
  ADD COLUMN access_scope VARCHAR(32) NOT NULL DEFAULT 'inherit' AFTER source_type,
  ADD COLUMN sensitive_level VARCHAR(32) NOT NULL DEFAULT 'normal' AFTER access_scope;

CREATE TABLE kb_document_acl (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  principal_type VARCHAR(32) NOT NULL,
  principal_value VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_kb_document_acl (tenant_id, document_id, principal_type, principal_value, deleted),
  KEY idx_kb_document_acl_document (tenant_id, document_id),
  KEY idx_kb_document_acl_principal (tenant_id, principal_type, principal_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
