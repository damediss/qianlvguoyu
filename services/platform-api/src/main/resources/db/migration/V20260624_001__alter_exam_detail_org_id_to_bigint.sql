-- 公司试题管理：org_id INT → BIGINT（与 sys_org.id 雪花ID对齐）
ALTER TABLE exam_exam_detail MODIFY COLUMN org_id BIGINT NULL COMMENT '组织ID';

-- 公司维度查询索引
CREATE INDEX idx_ed_org ON exam_exam_detail(tenant_id, org_id);
