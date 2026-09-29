-- ============================================================
-- 一次性数据迁移：把已有记录的 cdn.app.gjjd168.com URL 改成
-- 浏览器能访问的 http://localhost:8080/api/exam/files/...
-- ============================================================
-- 空库幂等化（踩坑 #34 修复）：本迁移的 UPDATE 同时引用 14 个 PHP 老表列
-- （social_security / contact_* / title_paper 等），链上 0608_001 只补了其中一部分，
-- 不能按单列判在。以 social_security 为"完整 legacy 形态"判别列（它只在 PHP 老表出现，
-- 链上任何迁移都不加）：缺它即说明不是完整老表，空库没有 legacy 数据可迁移，整体跳过。
-- 存量库本版本已应用不会重跑（validate-on-migrate=false）。
SET @db := DATABASE();
SET @has_legacy := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self'
    AND COLUMN_NAME = 'social_security');

SET @sql := IF(@has_legacy > 0,
  'UPDATE train_sign_up_people_self
   SET
     id_card_front       = REPLACE(id_card_front,       ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     id_card_back        = REPLACE(id_card_back,        ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     id_card_header      = REPLACE(id_card_header,      ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     id_card_header2     = REPLACE(id_card_header2,     ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     school_paper        = REPLACE(school_paper,        ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     title_paper         = REPLACE(title_paper,         ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     work_type_paper     = REPLACE(work_type_paper,     ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     contact_first_page  = REPLACE(contact_first_page,  ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     contact_expire_page = REPLACE(contact_expire_page, ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     contact_sign_page   = REPLACE(contact_sign_page,   ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     social_security     = REPLACE(social_security,     ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     photo               = REPLACE(photo,               ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     contract            = REPLACE(contract,            ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files''),
     certificate         = REPLACE(certificate,         ''https://cdn.app.gjjd168.com'', ''http://localhost:8080/api/exam/files'')
   WHERE id_card_front LIKE ''https://cdn.app.gjjd168.com/%''
      OR id_card_back  LIKE ''https://cdn.app.gjjd168.com/%''
      OR id_card_header LIKE ''https://cdn.app.gjjd168.com/%''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证（legacy 形态才查 id_card_front；空库输出 skipped）
SET @sql := IF(@has_legacy > 0,
  'SELECT id, sign_up_id, LEFT(id_card_front, 80) AS id_card_front_preview
   FROM train_sign_up_people_self
   WHERE id_card_front LIKE ''%/api/exam/files/%'' LIMIT 5',
  'SELECT ''skipped: slim schema'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
