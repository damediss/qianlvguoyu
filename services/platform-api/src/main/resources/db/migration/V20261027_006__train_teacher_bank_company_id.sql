-- 教务管理（师资库）按企业隔离：补充归属企业 ID，并按工作单位名称回填

ALTER TABLE train_teacher_bank
  ADD COLUMN company_id BIGINT NULL COMMENT '归属企业组织ID（sys_org）' AFTER company;

CREATE INDEX idx_train_teacher_bank_company ON train_teacher_bank (tenant_id, company_id, deleted);

-- 按工作单位名称匹配企业组织回填（仅 company/master）
UPDATE train_teacher_bank t
INNER JOIN sys_org o
  ON o.tenant_id = t.tenant_id
 AND o.deleted = 0
 AND o.org_type IN ('company', 'master')
 AND o.name = t.company
SET t.company_id = o.id
WHERE t.company_id IS NULL
  AND t.deleted = 0
  AND t.company IS NOT NULL
  AND TRIM(t.company) <> '';
