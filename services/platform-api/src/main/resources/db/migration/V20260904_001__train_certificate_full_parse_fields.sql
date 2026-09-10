-- 证书完整解析：职业名称 / 评价机构 / 证件类型（证面原文落库）
ALTER TABLE train_certificate
  ADD COLUMN occupation VARCHAR(255) NULL COMMENT '职业名称（证面）' AFTER work_type,
  ADD COLUMN evaluation_org VARCHAR(255) NULL COMMENT '评价机构/落款机构' AFTER issuing_authority,
  ADD COLUMN id_doc_type VARCHAR(64) NULL COMMENT '证件类型（证面，如居民身份证）' AFTER user_name;
