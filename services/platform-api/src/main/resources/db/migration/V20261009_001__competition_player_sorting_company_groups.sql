-- 排序分组改为按参赛公司分组，并保存组内角色与自定义工位编号。
-- 取号：测试库与生产库 flyway max 均为 20261008.003，共用版本 20261009.001。
ALTER TABLE competition_practical_sorting_task
  ADD COLUMN workstation_label VARCHAR(32) NULL COMMENT '工位自定义名称，如槽号、极号' AFTER result_snapshot_json,
  ADD COLUMN workstation_codes_json JSON NULL COMMENT '已创建的工位编号' AFTER workstation_label,
  ADD COLUMN preparation_json JSON NULL COMMENT '按参赛公司分组后的角色准备数据' AFTER workstation_codes_json;
