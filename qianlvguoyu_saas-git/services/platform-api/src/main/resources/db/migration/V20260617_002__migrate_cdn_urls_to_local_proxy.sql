-- ============================================================
-- 一次性数据迁移：把已有记录的 cdn.app.gjjd168.com URL 改成
-- 浏览器能访问的 http://localhost:8080/api/exam/files/...
-- ============================================================
UPDATE train_sign_up_people_self
SET
  id_card_front       = REPLACE(id_card_front,       'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  id_card_back        = REPLACE(id_card_back,        'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  id_card_header      = REPLACE(id_card_header,      'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  id_card_header2     = REPLACE(id_card_header2,     'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  school_paper        = REPLACE(school_paper,        'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  title_paper         = REPLACE(title_paper,         'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  work_type_paper     = REPLACE(work_type_paper,     'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  contact_first_page  = REPLACE(contact_first_page,  'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  contact_expire_page = REPLACE(contact_expire_page, 'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  contact_sign_page   = REPLACE(contact_sign_page,   'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  social_security     = REPLACE(social_security,     'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  photo               = REPLACE(photo,               'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  contract            = REPLACE(contract,            'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files'),
  certificate         = REPLACE(certificate,         'https://cdn.app.gjjd168.com', 'http://localhost:8080/api/exam/files')
WHERE id_card_front LIKE 'https://cdn.app.gjjd168.com/%'
   OR id_card_back  LIKE 'https://cdn.app.gjjd168.com/%'
   OR id_card_header LIKE 'https://cdn.app.gjjd168.com/%';

-- 验证
SELECT id, sign_up_id,
       LEFT(id_card_front, 80) AS id_card_front_preview
FROM train_sign_up_people_self
WHERE id_card_front LIKE '%/api/exam/files/%'
LIMIT 5;
