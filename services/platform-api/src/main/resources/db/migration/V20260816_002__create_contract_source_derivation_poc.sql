-- Version selected after querying flyway_schema_history: max=20260816.001 on 2026-08-14.
-- Contract source derivation POC: immutable DOCX snapshots and versioned structure manifests.

CREATE TABLE IF NOT EXISTS biz_contract_source_snapshot (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  provider VARCHAR(40) NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  knowledge_id VARCHAR(64) NOT NULL,
  source_file_id BIGINT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  source_mime_type VARCHAR(160) NOT NULL,
  source_file_hash CHAR(64) NOT NULL,
  source_file_size BIGINT NOT NULL,
  derivation_status VARCHAR(32) NOT NULL,
  source_parse_status VARCHAR(32) NULL,
  source_enable_status VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_source_knowledge_hash (
    tenant_id, provider, knowledge_id, source_file_hash, deleted
  ),
  KEY idx_contract_source_kb (tenant_id, knowledge_base_id, derivation_status, deleted),
  KEY idx_contract_source_file (tenant_id, source_file_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Immutable historical DOCX snapshots selected from the knowledge base';

CREATE TABLE IF NOT EXISTS biz_contract_source_manifest (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  source_snapshot_id BIGINT NOT NULL,
  version INT NOT NULL,
  manifest_json JSON NOT NULL,
  anchor_count INT NOT NULL DEFAULT 0,
  warning_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  renderer_version VARCHAR(40) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_source_manifest_version (
    tenant_id, source_snapshot_id, version, deleted
  ),
  KEY idx_contract_source_manifest_status (tenant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Versioned structure anchors for deterministic historical DOCX derivation';
