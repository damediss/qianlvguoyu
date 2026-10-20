-- 去重唯一键（方案 v2.1 §4.3）：
-- 键含 deleted 列，刻意保留旧语义“删除后可重传同文件”（软删行 deleted=1 与新行 deleted=0 不冲突；
-- 若无 deleted 列，软删行会把同键重传永久挡住，属于对 D5 的隐性回归）。
-- 重复“删除→重传→再删”撞键时由应用归档 scope_key 兜底（ZhentiAdminService.delete，v2.1 实现要点）。
-- 幂等：先查活跃重复与索引存在性，有冲突则不硬加（避免迁移炸库，踩坑 #34 手法）。

SET @dup := (SELECT COUNT(*) FROM (
  SELECT 1 FROM exam_zhenti
  WHERE deleted = 0
  GROUP BY tenant_id, scope_key, file_md5
  HAVING COUNT(*) > 1
  LIMIT 1) d);

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_zhenti' AND INDEX_NAME = 'uk_zhenti_scope_md5');

SET @ddl := IF(@dup = 0 AND @idx_exists = 0,
  'ALTER TABLE exam_zhenti ADD UNIQUE KEY uk_zhenti_scope_md5 (tenant_id, scope_key, file_md5, deleted)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
