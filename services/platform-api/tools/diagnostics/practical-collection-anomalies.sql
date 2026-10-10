-- 只读历史异常诊断。按当前租户/任务条件补充 WHERE 条件后执行。
SELECT
  c.tenant_id,
  c.task_id,
  c.id AS candidate_id,
  c.candidate_name,
  c.admission_ticket_no,
  s.overall_status,
  s.fingerprint_status,
  s.signature_status,
  GROUP_CONCAT(CASE WHEN d.doc_code IS NULL OR d.file_id IS NULL OR d.status <> 'approved' THEN codes.doc_code END ORDER BY codes.doc_code) AS incomplete_documents,
  GROUP_CONCAT(CASE WHEN d.doc_code IS NOT NULL AND (f.id IS NULL OR f.status <> 'uploaded' OR f.deleted <> 0) THEN d.doc_code END ORDER BY d.doc_code) AS invalid_file_records,
  fp.fingerprint_image_file_id,
  sig.signature_file_id
FROM certification_candidate c
LEFT JOIN certification_practical_collection_status s
  ON s.tenant_id = c.tenant_id AND s.task_id = c.task_id AND s.candidate_id = c.id AND s.deleted = 0
JOIN (
  SELECT 'DOC001' AS doc_code UNION ALL SELECT 'DOC002' UNION ALL SELECT 'DOC003' UNION ALL SELECT 'DOC004'
) codes ON 1 = 1
LEFT JOIN certification_candidate_document d
  ON d.tenant_id = c.tenant_id AND d.task_id = c.task_id AND d.candidate_id = c.id
 AND d.doc_code = codes.doc_code AND d.deleted = 0
LEFT JOIN sys_file f ON f.tenant_id = c.tenant_id AND f.id = d.file_id
LEFT JOIN certification_practical_fingerprint_trace fp
  ON fp.tenant_id = c.tenant_id AND fp.task_id = c.task_id AND fp.candidate_id = c.id
 AND fp.deleted = 0 AND fp.id = (
   SELECT fp2.id FROM certification_practical_fingerprint_trace fp2
   WHERE fp2.tenant_id = c.tenant_id AND fp2.task_id = c.task_id AND fp2.candidate_id = c.id AND fp2.deleted = 0
   ORDER BY fp2.collected_at DESC, fp2.created_at DESC LIMIT 1
 )
LEFT JOIN certification_practical_signature_record sig
  ON sig.tenant_id = c.tenant_id AND sig.task_id = c.task_id AND sig.candidate_id = c.id
 AND sig.record_status = 'ACTIVE' AND sig.deleted = 0 AND sig.id = (
   SELECT sig2.id FROM certification_practical_signature_record sig2
   WHERE sig2.tenant_id = c.tenant_id AND sig2.task_id = c.task_id AND sig2.candidate_id = c.id
     AND sig2.record_status = 'ACTIVE' AND sig2.deleted = 0
   ORDER BY sig2.signed_at DESC, sig2.created_at DESC LIMIT 1
 )
WHERE c.deleted = 0
GROUP BY c.tenant_id, c.task_id, c.id, c.candidate_name, c.admission_ticket_no,
         s.overall_status, s.fingerprint_status, s.signature_status,
         fp.fingerprint_image_file_id, sig.signature_file_id
HAVING (s.id IS NOT NULL AND (
  incomplete_documents IS NOT NULL
  OR invalid_file_records IS NOT NULL
  OR (s.overall_status = 'COMPLETED' AND fp.fingerprint_image_file_id IS NULL)
  OR (s.overall_status = 'COMPLETED' AND sig.signature_file_id IS NULL)
));
