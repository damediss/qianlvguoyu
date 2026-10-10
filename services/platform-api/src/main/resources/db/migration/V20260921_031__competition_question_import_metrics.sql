-- 竞赛试题导入分段耗时。失败批次也可保留已测到的 parse/validate 数据。
ALTER TABLE competition_question_import_batch
  ADD COLUMN metrics_json TEXT NULL COMMENT '导入分段耗时' AFTER error_detail;
