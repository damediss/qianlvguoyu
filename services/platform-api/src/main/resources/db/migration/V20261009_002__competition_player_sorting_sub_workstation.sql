-- 工位抽签增加子工位，例如槽号下的极号。
-- 取号：共用目录最新为 20261009.001，本次 20261009.002。
ALTER TABLE competition_practical_sorting_task
  ADD COLUMN sub_workstation_label VARCHAR(32) NULL COMMENT '子工位自定义描述' AFTER preparation_json,
  ADD COLUMN sub_workstation_codes_json JSON NULL COMMENT '子工位编号' AFTER sub_workstation_label;
