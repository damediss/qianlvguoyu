-- 修复 train_info_list 历史数据 create_time 为 NULL 的问题
UPDATE train_info_list SET create_time = NOW() WHERE create_time IS NULL;

-- 为新记录设置默认值
ALTER TABLE train_info_list MODIFY COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
