-- 细目表批次留存源文件：导入时经文件服务(sys_file)保存上传的 xlsx，列表提供"细目表下载"，
-- 供权重校验未通过等场景下载源表核对。历史批次该列为 NULL，下载按钮置灰提示重新导入。

ALTER TABLE exam_theory_blueprint
  ADD COLUMN source_file_id BIGINT NULL COMMENT '源细目表文件ID（sys_file.id），导入时留存' AFTER source_file_name;
