-- 实操采集准入重新统一要求 DOC001-DOC004。
-- 不恢复历史已删除的附件记录；历史考生需在信息收集页重新补齐并审核通过。
INSERT INTO certification_material_template (
  id, tenant_id, template_scope, item_code, item_name, item_group, required,
  accept_file_types, max_file_size_mb, enabled, sort_order, remark,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202607130101, 1, 'candidate_doc', 'DOC003', '身份证正面', '基础资料', 1, '.pdf,.jpg,.png', 50, 1, 3, NULL, NOW(), NOW(), 1, 1, 0),
  (202607130102, 1, 'candidate_doc', 'DOC004', '身份证反面', '基础资料', 1, '.pdf,.jpg,.png', 50, 1, 4, NULL, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  required = 1,
  enabled = 1,
  accept_file_types = VALUES(accept_file_types),
  max_file_size_mb = VALUES(max_file_size_mb),
  updated_at = NOW(),
  deleted = 0;

UPDATE certification_candidate
SET document_required_count = 4,
    document_completed_count = (
      SELECT COUNT(*)
      FROM certification_candidate_document d
      WHERE d.tenant_id = certification_candidate.tenant_id
        AND d.task_id = certification_candidate.task_id
        AND d.candidate_id = certification_candidate.id
        AND d.deleted = 0
        AND d.doc_code IN ('DOC001', 'DOC002', 'DOC003', 'DOC004')
        AND d.file_id IS NOT NULL
        AND d.status = 'approved'
    ),
    document_complete = (
      SELECT CASE WHEN COUNT(*) = 4
                       AND SUM(CASE WHEN d.file_id IS NOT NULL AND d.status = 'approved' THEN 1 ELSE 0 END) = 4
                  THEN 1 ELSE 0 END
      FROM certification_candidate_document d
      WHERE d.tenant_id = certification_candidate.tenant_id
        AND d.task_id = certification_candidate.task_id
        AND d.candidate_id = certification_candidate.id
        AND d.deleted = 0
        AND d.doc_code IN ('DOC001', 'DOC002', 'DOC003', 'DOC004')
    )
WHERE deleted = 0;
