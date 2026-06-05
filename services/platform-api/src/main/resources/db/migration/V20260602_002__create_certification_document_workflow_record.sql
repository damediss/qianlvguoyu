CREATE TABLE IF NOT EXISTS certification_document_workflow_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
  task_id BIGINT NOT NULL COMMENT 'certification task id',
  document_id BIGINT NOT NULL COMMENT 'certification task document id',
  document_type VARCHAR(40) NOT NULL COMMENT 'document type',
  process_instance_id VARCHAR(128) NULL COMMENT 'Flowable process instance id',
  task_id_in_engine VARCHAR(128) NULL COMMENT 'Flowable task id',
  node_key VARCHAR(80) NULL COMMENT 'BPMN node key',
  node_name VARCHAR(120) NULL COMMENT 'BPMN node name',
  action VARCHAR(40) NOT NULL COMMENT 'submit, initial_review, final_review, archive',
  action_result VARCHAR(40) NOT NULL COMMENT 'submitted, approved, rejected, archived',
  comment VARCHAR(1000) NULL COMMENT 'operator comment',
  operator_user_id BIGINT NULL COMMENT 'operator user id',
  operator_name VARCHAR(120) NULL COMMENT 'operator display name',
  operated_at DATETIME NOT NULL COMMENT 'operation time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_certification_doc_wf_doc (tenant_id, document_id, operated_at, deleted),
  KEY idx_certification_doc_wf_process (tenant_id, process_instance_id, deleted),
  KEY idx_certification_doc_wf_task (tenant_id, task_id, document_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='certification document workflow record';

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6944, 1, 6301, 'certification.project.document.submit', 'Submit certification document workflow', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6945, 1, 6301, 'certification.project.document.initialReview', 'Initial review certification document', 'approve', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6946, 1, 6301, 'certification.project.document.finalReview', 'Final review certification document', 'approve', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6947, 1, 6301, 'certification.project.document.workflowView', 'View certification document workflow', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);
