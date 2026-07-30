-- V20260610_002: 知识比重主表 + 详情表
-- 对标 gjjd-java exam_knowledge_rate / exam_knowledge_rate_detail

CREATE TABLE IF NOT EXISTS exam_knowledge_rate (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  course_id INT NULL COMMENT '科目ID → exam_course.id',
  type INT NULL DEFAULT 0 COMMENT '类型',
  is_standard INT NULL DEFAULT 0 COMMENT '是否标准比重: 0=否, 1=是',
  title VARCHAR(255) NULL COMMENT '标题',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号(越小越靠前)',
  comments VARCHAR(500) NULL COMMENT '备注',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=冻结, 1=正常',
  user_id INT NULL COMMENT '创建人',
  deleted INT NULL DEFAULT 0 COMMENT '逻辑删除: 0=否, 1=是',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_knowledge_rate_course (tenant_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识比重';

CREATE TABLE IF NOT EXISTS exam_knowledge_rate_detail (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  knowledge_rate_id INT NOT NULL COMMENT '比重ID → exam_knowledge_rate.id',
  knowledge_id INT NULL COMMENT '知识点ID → exam_knowledge_structure.id',
  rate INT NULL DEFAULT 0 COMMENT '比重百分比(%)',
  user_id INT NULL COMMENT '创建人',
  sort_number INT NULL DEFAULT 1 COMMENT '排序号',
  comments VARCHAR(500) NULL COMMENT '备注',
  status INT NULL DEFAULT 1 COMMENT '状态: 0=冻结, 1=正常',
  deleted INT NULL DEFAULT 0 COMMENT '逻辑删除: 0=否, 1=是',
  tenant_id INT NOT NULL COMMENT '租户ID',
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_rate_detail_rate (tenant_id, knowledge_rate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识比重详情';

