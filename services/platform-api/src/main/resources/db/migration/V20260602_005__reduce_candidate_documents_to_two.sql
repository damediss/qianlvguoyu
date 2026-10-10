-- 信息收集环节考生资料仅保留：报名信息表(DOC001)、两寸证件照(DOC002)

UPDATE certification_material_template
SET enabled = 0,
    deleted = 1,
    updated_at = NOW()
WHERE template_scope = 'candidate_doc'
  AND item_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 0;

UPDATE certification_candidate_document
SET deleted = 1,
    updated_at = NOW()
WHERE doc_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 0;

UPDATE certification_candidate
SET document_required_count = 2,
    document_completed_count = (
      SELECT COUNT(*)
      FROM certification_candidate_document d
      WHERE d.candidate_id = certification_candidate.id
        AND d.deleted = 0
        AND d.doc_code IN ('DOC001', 'DOC002')
        AND d.required = 1
        AND d.status = 'approved'
    ),
    document_complete = (
      SELECT CASE
        WHEN COUNT(*) = 0 THEN 0
        WHEN SUM(CASE WHEN d.status = 'approved' THEN 1 ELSE 0 END) = COUNT(*) THEN 1
        ELSE 0
      END
      FROM certification_candidate_document d
      WHERE d.candidate_id = certification_candidate.id
        AND d.deleted = 0
        AND d.doc_code IN ('DOC001', 'DOC002')
        AND d.required = 1
    )
WHERE deleted = 0;

ALTER TABLE certification_candidate
  MODIFY COLUMN document_required_count INT NOT NULL DEFAULT 2 COMMENT '必需资料数';
