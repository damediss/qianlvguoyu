-- ============================================================================
-- train_exam_list 一次性导入脚本（PHP 库 gjjddb → SaaS 库 gjjddb2026）
-- 对应文档：docs/04_requirements/AI同步学统计功能设计方案.md v1.1 §3（拍板⑤⑥⑦）
--
-- 规则：
--   1. 全量导入（含 deleted=1 行，保持 id 全集一致，查询侧按 deleted=0 过滤）
--   2. 同 id 冲突行以 PHP 内容为准整行覆盖（ON DUPLICATE KEY UPDATE ... VALUES()）
--   3. SaaS 有而 PHP 没有的 id（本地创建行）保留不动
--   4. tenant_id 恒填 1（PHP 侧为 10159，SaaS 统一租户为 1，否则多租户插件查不到）
--   5. jigou_id 填 NULL（PHP 机构 id 对 SaaS 无意义，防污染 SaaS 考试管理筛选）
--   6. update_time 填 NOW()（SaaS 独有列）
--   7. 导入后 AUTO_INCREMENT=100000：id<100000 即 PHP 来源行（只读，见方案 §3.3）
--
-- 执行方式（两段）：
--   第 1 段【PHP 库执行】生成本次导入的 upsert 语句文件：
--     mysql -h 114.132.67.185 -P 3308 -u root -p gjjddb -N -B \
--       < infra/db/train-exam-list-import.sql > /tmp/train_exam_list_upsert.sql
--     （本文件只有一条生成器 SELECT，-N -B 输出即为可执行 SQL）
--   第 2 段【SaaS 库执行】导入 + 抬升 id 区间：
--     mysql -h 43.138.163.7 -P 3308 -u gjjd -p gjjddb2026 < /tmp/train_exam_list_upsert.sql
--     mysql -h 43.138.163.7 -P 3308 -u gjjd -p gjjddb2026 \
--       -e "ALTER TABLE train_exam_list AUTO_INCREMENT = 100000;"
--
-- 验证（SaaS 库）：
--   SELECT COUNT(*) FROM train_exam_list;                  -- >= PHP 侧 458（含 SaaS 本地行）
--   SELECT COUNT(*) FROM train_exam_list WHERE tenant_id<>1; -- 必须为 0
--   SELECT id, title FROM train_exam_list WHERE id=440;     -- 应为 PHP 侧标题
--   SHOW TABLE STATUS LIKE 'train_exam_list';               -- Auto_increment >= 100000
--
-- 重跑幂等：同规则再覆盖一遍，无脏数据风险。PHP 新建复习计划后需人肉重跑本脚本。
-- ============================================================================

-- 第 1 段：在 PHP 库执行（gjjddb）。QUOTE() 负责转义与 NULL 处理，列序固定。
SELECT CONCAT(
  'INSERT INTO train_exam_list (',
  'id, title, level, train_id, org_id, org_name, type, source, ',
  'invigilate_user_id, invigilate_username, evaluate_user_id, evaluate_username, ',
  'watcher_user_id, watcher_username, staff_user_id, staff_username, ',
  'proposition_user_id, proposition_username, marking_user_id, marking_username, ',
  'exam_id, exam_date, exam_date_mode, exam_date_list, exam_time, ',
  'need_finish_adventure, need_finish_normal, user_id, deleted, sort_number, create_time, ',
  'tenant_id, jigou_id, update_time',
  ') VALUES (',
  QUOTE(id), ',', QUOTE(title), ',', QUOTE(level), ',', QUOTE(train_id), ',',
  QUOTE(org_id), ',', QUOTE(org_name), ',', QUOTE(type), ',', QUOTE(source), ',',
  QUOTE(invigilate_user_id), ',', QUOTE(invigilate_username), ',',
  QUOTE(evaluate_user_id), ',', QUOTE(evaluate_username), ',',
  QUOTE(watcher_user_id), ',', QUOTE(watcher_username), ',',
  QUOTE(staff_user_id), ',', QUOTE(staff_username), ',',
  QUOTE(proposition_user_id), ',', QUOTE(proposition_username), ',',
  QUOTE(marking_user_id), ',', QUOTE(marking_username), ',',
  QUOTE(exam_id), ',', QUOTE(exam_date), ',', QUOTE(exam_date_mode), ',',
  QUOTE(exam_date_list), ',', QUOTE(exam_time), ',',
  QUOTE(need_finish_adventure), ',', QUOTE(need_finish_normal), ',',
  QUOTE(user_id), ',', QUOTE(deleted), ',', QUOTE(sort_number), ',', QUOTE(create_time), ',',
  '1, NULL, NOW()',
  ') ON DUPLICATE KEY UPDATE ',
  'title=VALUES(title), level=VALUES(level), train_id=VALUES(train_id), ',
  'org_id=VALUES(org_id), org_name=VALUES(org_name), type=VALUES(type), source=VALUES(source), ',
  'invigilate_user_id=VALUES(invigilate_user_id), invigilate_username=VALUES(invigilate_username), ',
  'evaluate_user_id=VALUES(evaluate_user_id), evaluate_username=VALUES(evaluate_username), ',
  'watcher_user_id=VALUES(watcher_user_id), watcher_username=VALUES(watcher_username), ',
  'staff_user_id=VALUES(staff_user_id), staff_username=VALUES(staff_username), ',
  'proposition_user_id=VALUES(proposition_user_id), proposition_username=VALUES(proposition_username), ',
  'marking_user_id=VALUES(marking_user_id), marking_username=VALUES(marking_username), ',
  'exam_id=VALUES(exam_id), exam_date=VALUES(exam_date), exam_date_mode=VALUES(exam_date_mode), ',
  'exam_date_list=VALUES(exam_date_list), exam_time=VALUES(exam_time), ',
  'need_finish_adventure=VALUES(need_finish_adventure), need_finish_normal=VALUES(need_finish_normal), ',
  'user_id=VALUES(user_id), deleted=VALUES(deleted), sort_number=VALUES(sort_number), ',
  'create_time=VALUES(create_time), tenant_id=1, jigou_id=NULL, update_time=NOW();'
) AS upsert_sql
FROM train_exam_list
ORDER BY id;
