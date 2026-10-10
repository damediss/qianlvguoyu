ALTER TABLE certification_practical_collection_status
  ADD COLUMN exam_session_id BIGINT NULL COMMENT 'certification exam session id' AFTER session_id,
  ADD KEY idx_cert_practical_collect_status_exam_session (tenant_id, exam_session_id, deleted);

ALTER TABLE certification_practical_collection_queue
  ADD COLUMN exam_session_id BIGINT NULL COMMENT 'certification exam session id' AFTER task_id,
  ADD KEY idx_cert_practical_collect_queue_exam_session (tenant_id, exam_session_id, deleted);

ALTER TABLE certification_practical_fingerprint_trace
  ADD COLUMN exam_session_id BIGINT NULL COMMENT 'certification exam session id' AFTER session_id,
  ADD KEY idx_cert_practical_fingerprint_exam_session (tenant_id, exam_session_id, deleted);

ALTER TABLE certification_practical_signature_record
  ADD COLUMN exam_session_id BIGINT NULL COMMENT 'certification exam session id' AFTER session_id,
  ADD KEY idx_cert_practical_signature_exam_session (tenant_id, exam_session_id, deleted);

ALTER TABLE certification_practical_collection_exception
  ADD COLUMN exam_session_id BIGINT NULL COMMENT 'certification exam session id' AFTER task_id,
  ADD KEY idx_cert_practical_collect_exception_exam_session (tenant_id, exam_session_id, deleted);
