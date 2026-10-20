-- 回填存量真题的共享组（方案 v2.1 §4.4）：
-- 按 exam_id 的活跃有效绑定写 work_type_id/level_code/scope_key；
-- 无有效绑定的行写 scope_key = 'exam:{exam_id}'（降级为计划私有，与运行时 resolver 口径一致）。
-- 幂等：只处理 scope_key = '' 的行，重复执行无副作用。
-- 多绑定计划取 MIN(work_type_id)/MIN(level) 的代表值（运行时是并集语义，回填取其一即可）。

UPDATE exam_zhenti z
LEFT JOIN (
  SELECT w.exam_id, MIN(w.work_type_id) AS work_type_id, MIN(w.level) AS level
  FROM train_exam_work_type_list w
  JOIN train_exam_list e ON e.id = w.exam_id AND e.deleted = 0
  JOIN exam_work_type t ON t.id = w.work_type_id AND t.deleted = 0
  WHERE w.deleted = 0 AND w.work_type_id IS NOT NULL AND w.level IS NOT NULL
  GROUP BY w.exam_id
) v ON v.exam_id = z.exam_id
SET z.work_type_id = v.work_type_id,
    z.level_code = v.level,
    z.scope_key = IF(v.exam_id IS NULL, CONCAT('exam:', z.exam_id), CONCAT('work:', v.work_type_id, ':', v.level))
WHERE z.scope_key = '';
