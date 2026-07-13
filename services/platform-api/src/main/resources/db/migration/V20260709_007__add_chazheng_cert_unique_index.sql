-- 为 chazheng_cert 表增加唯一索引，防止并发查证时重复插入同一用户的同一证书
SET @db := DATABASE();
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'chazheng_cert'
   AND INDEX_NAME = 'uk_chazheng_cert_user_cert_no') = 0,
  'ALTER TABLE chazheng_cert ADD UNIQUE KEY uk_chazheng_cert_user_cert_no (user_id, cert_no)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
