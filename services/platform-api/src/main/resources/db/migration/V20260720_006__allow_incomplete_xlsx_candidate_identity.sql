ALTER TABLE certification_candidate
  MODIFY COLUMN candidate_name VARCHAR(80) NULL COMMENT '姓名',
  MODIFY COLUMN id_card_no VARCHAR(32) NULL COMMENT '身份证号';

ALTER TABLE certification_candidate_source_staging
  MODIFY COLUMN candidate_name VARCHAR(80) NULL COMMENT '姓名 | {{candidate_name}} | Excel:name | cert.candidate_name',
  MODIFY COLUMN id_card_no VARCHAR(32) NULL COMMENT '身份证号 | {{id_card_no}} | Excel:id_card_number | cert.id_card_no';
