-- App 刷题：存量 PHP 表 id 常无自增，补 AUTO_INCREMENT（Druid wall 禁止 PREPARE，用直接 ALTER）
-- 代码侧已有 MAX(id)+1 兜底；本迁移便于长期与 IdType.AUTO 对齐

ALTER TABLE exam_mock_paper
  MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE exam_user_answer_log
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
