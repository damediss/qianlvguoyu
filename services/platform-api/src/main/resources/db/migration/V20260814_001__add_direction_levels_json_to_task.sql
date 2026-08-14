-- 职业技能标准解析任务：增加工种可用等级矩阵（1.4 节解析结果），用于前端过滤 tab。
ALTER TABLE exam_standard_task ADD COLUMN direction_levels_json TEXT NULL COMMENT '工种 -> 可用 levelCode 列表的 JSON，来自 docx 1.4 节';
