-- 班级刷题/练习范围：科目 + 知识点白名单（App 后接；后台先配）

CREATE TABLE IF NOT EXISTS train_class_practice_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL COMMENT 'train_info_class.id',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark VARCHAR(512) NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_class_practice_scope (tenant_id, class_id),
  KEY idx_class_practice_scope_class (class_id)
) COMMENT='班级练习范围主配置';

CREATE TABLE IF NOT EXISTS train_class_practice_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  exam_course_id INT NOT NULL COMMENT 'exam_course.id',
  create_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_class_practice_course (tenant_id, class_id, exam_course_id),
  KEY idx_class_practice_course_class (class_id)
) COMMENT='班级练习科目白名单';

CREATE TABLE IF NOT EXISTS train_class_practice_knowledge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  exam_course_id INT NOT NULL COMMENT '冗余科目，便于校验',
  knowledge_id INT NOT NULL COMMENT 'exam_knowledge_structure.id',
  create_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_class_practice_knowledge (tenant_id, class_id, knowledge_id),
  KEY idx_class_practice_knowledge_class (class_id),
  KEY idx_class_practice_knowledge_course (class_id, exam_course_id)
) COMMENT='班级练习知识点白名单；空表示所选科目下不限知识点';
