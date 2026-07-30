CREATE TABLE IF NOT EXISTS exam_user_auth_course (
  id INT NOT NULL AUTO_INCREMENT,
  uid INT NULL DEFAULT NULL,
  realname VARCHAR(255) NULL DEFAULT NULL COMMENT '专家姓名',
  course_id INT NULL DEFAULT NULL,
  course_title VARCHAR(255) NULL DEFAULT NULL COMMENT '科目名称',
  type TINYINT(1) DEFAULT 0 COMMENT '0命题科目 1审题科目 2终审科目',
  user_id INT NULL DEFAULT NULL,
  id_card VARCHAR(50) NULL DEFAULT NULL COMMENT '身份证号',
  status INT NOT NULL DEFAULT 1 COMMENT '状态',
  sort_number INT NOT NULL DEFAULT 1 COMMENT '排序号',
  deleted INT NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户id',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_uid (uid),
  INDEX idx_course_id (course_id),
  INDEX idx_realname_idcard (realname, id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专家授权科目';
