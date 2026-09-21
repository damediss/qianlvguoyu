-- 试题配置「可刷科目」展示名：铝电解操作工 → 铝电解槽操作工（国标工种全称）
-- 仅替换旧简称，已含「槽」的标题不受影响。

UPDATE exam_course
SET title = REPLACE(title, '铝电解操作工', '铝电解槽操作工')
WHERE title LIKE '%铝电解操作工%';

UPDATE exam_work_type
SET title = REPLACE(title, '铝电解操作工', '铝电解槽操作工')
WHERE title LIKE '%铝电解操作工%';
