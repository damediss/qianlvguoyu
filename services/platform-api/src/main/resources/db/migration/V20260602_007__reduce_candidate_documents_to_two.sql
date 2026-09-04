-- 信息收集环节考生资料仅保留：报名信息表(DOC001)、两寸证件照(DOC002)
-- 本文件按生产库 flyway_schema_history(version=20260602.007, script=V20260602_007__reduce_candidate_documents_to_two.sql)
-- 回放重建。与 V20260602_005 的差异：gjjddb2026(测试库) 在 6 月 005 运行后，
-- 7 月迁移又为 DOC003/DOC004 新增了启用行（deleted=0），而 6 月墓碑（deleted=1）仍在；
-- 两表唯一键均含 deleted 列，直接 UPDATE 软删会撞唯一键（uk_cert_material_template_code /
-- uk_cert_candidate_doc_code）。故先硬删历史墓碑、再软删存活行，保证幂等重放。

-- 1) 清掉 DOC003/DOC004 等旧墓碑（逻辑上已删除的模板定义，硬删不影响业务数据）
DELETE FROM certification_material_template
WHERE template_scope = 'candidate_doc'
  AND item_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 1;

-- 2) 仍存活的旧模板软删（与 V20260602_005 语义一致）
UPDATE certification_material_template
SET enabled = 0,
    deleted = 1,
    updated_at = NOW()
WHERE template_scope = 'candidate_doc'
  AND item_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 0;

-- 3) 考生已上传的旧资料行：先清墓碑、再软删存活行（同唯一键防御）
DELETE FROM certification_candidate_document
WHERE doc_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 1;

UPDATE certification_candidate_document
SET deleted = 1,
    updated_at = NOW()
WHERE doc_code NOT IN ('DOC001', 'DOC002')
  AND deleted = 0;

-- 4) 重算考生资料完成度
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
