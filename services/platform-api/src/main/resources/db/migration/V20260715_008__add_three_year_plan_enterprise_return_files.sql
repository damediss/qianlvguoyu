-- 三年规划：企业回传筛选名单 + 排班表
-- 共享开发库列已提前加好；Druid Wall 禁止 PREPARE 动态 DDL，故本版本记为空操作占位。
-- 全新空库若缺列，请手工执行同目录历史脚本中的 ALTER，或联系 DBA 补齐下列字段后启动：
--   enterprise_roster_file_id / enterprise_shift_file_id
--   enterprise_roster_file_name / enterprise_shift_file_name
--   enterprise_return_time / enterprise_return_user_id / enterprise_return_username
--   enterprise_return_history_json

SELECT 1;
