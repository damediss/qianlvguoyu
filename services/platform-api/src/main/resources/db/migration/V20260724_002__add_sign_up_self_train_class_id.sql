-- 开班准备：按班挂资料收集计划（一班一套）
-- collection_source 新增 class_prep；与 research / train_info 隔离

ALTER TABLE train_sign_up_self
  ADD COLUMN train_class_id BIGINT NULL COMMENT '开班准备资料收集绑定的班级 train_info_class.id' AFTER train_info_id;

ALTER TABLE train_sign_up_self
  MODIFY COLUMN collection_source VARCHAR(32) NOT NULL DEFAULT 'train_info'
    COMMENT 'train_info=旧信息收集 research=企业调研 class_prep=开班准备按班收集';

-- MySQL 允许多个 NULL，仅对已绑定班级保证租户内唯一
CREATE UNIQUE INDEX uk_sign_up_tenant_class
  ON train_sign_up_self (tenant_id, train_class_id);
