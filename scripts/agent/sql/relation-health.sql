-- relation-health.sql —— 考试计划 ↔ 工种/等级/课程 关联关系健康检查
-- 默认在测试库 gjjddb2026 执行；全部为只读 SELECT。
-- 口径说明：
--   1. 全部按 tenant_id 分列——多租户库不分组的统计回答不了"某租户多少计划有绑定"；
--   2. train_exam_work_type_list.level 实测为 tinyint(1) 存 0~4（0=五级 … 4=一级）；
--      迁移 V20260521_001 写的 VARCHAR(50) 是空转 DDL（CREATE IF NOT EXISTS 对存量
--      PHP 表不生效），列结构以 docs/agent/schema-index.train.json 实测为准。

-- 1. 计划绑定覆盖率（按租户）
SELECT e.tenant_id,
       COUNT(*) AS plans,
       SUM(w.exam_id IS NULL) AS no_binding,
       COUNT(DISTINCT w.exam_id) AS bound_plans
FROM train_exam_list e
LEFT JOIN (SELECT DISTINCT exam_id FROM train_exam_work_type_list WHERE deleted = 0) w
       ON w.exam_id = e.id
WHERE e.deleted = 0
GROUP BY e.tenant_id;

-- 2. 每计划绑定行数分布（按租户）
SELECT t.tenant_id, t.cnt AS bindings_per_plan, COUNT(*) AS plans
FROM (SELECT w.tenant_id, w.exam_id, COUNT(*) AS cnt
      FROM train_exam_work_type_list w
      WHERE w.deleted = 0
      GROUP BY w.tenant_id, w.exam_id) t
GROUP BY t.tenant_id, t.cnt
ORDER BY t.tenant_id, t.cnt;

-- 3. 关键列空值（按租户）
SELECT tenant_id,
       SUM(work_type_id IS NULL) AS null_work_type,
       SUM(level IS NULL) AS null_level,
       SUM(course_id IS NULL) AS null_course
FROM train_exam_work_type_list
WHERE deleted = 0
GROUP BY tenant_id;

-- 4. 等级分布（按租户；level 实测 tinyint(1)，0~4 = 五级~一级）
SELECT tenant_id, level, COUNT(*) AS cnt
FROM train_exam_work_type_list
WHERE deleted = 0
GROUP BY tenant_id, level
ORDER BY tenant_id, level;
