-- 对齐旧系统 file_template：扩展字段（MySQL 8.4 使用裸 ADD COLUMN）
ALTER TABLE train_file_temp
  ADD COLUMN ext VARCHAR(20) NULL COMMENT '扩展名' AFTER path_source;

ALTER TABLE train_file_temp
  ADD COLUMN path_used VARCHAR(500) NULL COMMENT '变量替换后的Word路径' AFTER ext;
