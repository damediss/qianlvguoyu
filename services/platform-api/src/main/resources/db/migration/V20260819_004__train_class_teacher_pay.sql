-- 跟班本班教师课酬（元/天），按老师+授课形式
CREATE TABLE IF NOT EXISTS train_class_teacher_pay (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  teacher_name VARCHAR(64) NOT NULL COMMENT '与课表 teacher_name 对齐（trim）',
  teach_form VARCHAR(32) NOT NULL DEFAULT '未填' COMMENT '理论/实操/未填',
  rate_per_day DECIMAL(12, 2) NULL COMMENT '课酬单价（元/天）',
  remark VARCHAR(255) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_class_teacher_form (tenant_id, class_id, teacher_name, teach_form, deleted),
  KEY idx_class_teacher_pay_class (class_id, deleted)
) COMMENT='跟班班级教师课酬配置';
