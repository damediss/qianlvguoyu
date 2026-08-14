-- ============================================================
-- 国标理论知识权重（type=3）—— 浸出工（职业：重冶湿法冶炼工）
-- 标准来源：《重冶湿法冶炼工国家职业技能标准（2020 年版）》4.1 理论知识权重表
-- 目标库：PHP 库 gjjddb
-- 生成说明：依据 exam_work_type_202608041129.md / exam_course_202608041130.md 中的现网数据，
--          仅对“浸出工”5 个等级（course_id 183~187）生成可落库 SQL。
--          同职业下的净化工（course_id 178~182）、蒸发浓缩结晶工、重冶萃取工、
--          有色液固分离工因缺少 exam_knowledge_structure L1 节点映射，未在本文件生成。
-- 注意：执行前请先确认目标库 exam_knowledge_rate 的 AUTO_INCREMENT 及 tenant_id 与现网一致。
-- ============================================================

-- 183 浸出工初级（五级）
INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)
VALUES (183, 3, '国标理论知识权重', 1, 1, '重冶湿法冶炼工国家职业技能标准 4.1 理论知识权重表（五级）', 1, NULL, 0, 10159);
SET @rid := LAST_INSERT_ID();
INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id) VALUES
(@rid, 5526,  5, 1, '职业道德', 1, 0, 10159),
(@rid, 5539, 35, 2, '基础知识', 1, 0, 10159),
(@rid, 5616, 40, 3, '湿法冶炼（树节点：物料浸出）', 1, 0, 10159),
(@rid, 5695, 10, 4, '设备检修与维护', 1, 0, 10159),
(@rid, 5718, 10, 5, '安全环保', 1, 0, 10159);

-- 184 浸出工中级（四级）
INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)
VALUES (184, 3, '国标理论知识权重', 1, 1, '重冶湿法冶炼工国家职业技能标准 4.1 理论知识权重表（四级）', 1, NULL, 0, 10159);
SET @rid := LAST_INSERT_ID();
INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id) VALUES
(@rid, 24311,  5, 1, '职业道德', 1, 0, 10159),
(@rid, 24324, 30, 2, '基础知识', 1, 0, 10159),
(@rid, 24392, 40, 3, '湿法冶炼（树节点：物料浸出）', 1, 0, 10159),
(@rid, 24475, 15, 4, '设备检修与维护', 1, 0, 10159),
(@rid, 24508, 10, 5, '安全环保', 1, 0, 10159);

-- 185 浸出工高级（三级）
INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)
VALUES (185, 3, '国标理论知识权重', 1, 1, '重冶湿法冶炼工国家职业技能标准 4.1 理论知识权重表（三级）', 1, NULL, 0, 10159);
SET @rid := LAST_INSERT_ID();
INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id) VALUES
(@rid, 6010,  5, 1, '职业道德', 1, 0, 10159),
(@rid, 6023, 20, 2, '基础知识', 1, 0, 10159),
(@rid, 6071, 35, 3, '湿法冶炼（树节点：物料浸出）', 1, 0, 10159),
(@rid, 6135, 20, 4, '设备检修与维护', 1, 0, 10159),
(@rid, 6178, 10, 5, '安全环保', 1, 0, 10159),
(@rid, 6201,  5, 6, '生产管理与技术创新', 1, 0, 10159),
(@rid, 6214,  5, 7, '培训与指导', 1, 0, 10159);

-- 186 浸出工技师（二级）
INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)
VALUES (186, 3, '国标理论知识权重', 1, 1, '重冶湿法冶炼工国家职业技能标准 4.1 理论知识权重表（二级）', 1, NULL, 0, 10159);
SET @rid := LAST_INSERT_ID();
INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id) VALUES
(@rid, 6267,  5, 1, '职业道德', 1, 0, 10159),
(@rid, 6280, 15, 2, '基础知识', 1, 0, 10159),
(@rid, 6318, 35, 3, '湿法冶炼', 1, 0, 10159),
(@rid, 6390, 15, 4, '设备检修与维护', 1, 0, 10159),
(@rid, 6423,  5, 5, '安全环保', 1, 0, 10159),
(@rid, 6436, 15, 6, '生产管理与技术创新', 1, 0, 10159),
(@rid, 6468, 10, 7, '培训与指导', 1, 0, 10159);

-- 187 浸出工高级技师（一级）
INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)
VALUES (187, 3, '国标理论知识权重', 1, 1, '重冶湿法冶炼工国家职业技能标准 4.1 理论知识权重表（一级）', 1, NULL, 0, 10159);
SET @rid := LAST_INSERT_ID();
INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id) VALUES
(@rid, 6534,  5, 1, '职业道德', 1, 0, 10159),
(@rid, 6547, 10, 2, '基础知识', 1, 0, 10159),
(@rid, 6575, 30, 3, '湿法冶炼', 1, 0, 10159),
(@rid, 6638, 15, 4, '设备检修与维护', 1, 0, 10159),
(@rid, 6669,  5, 5, '安全环保', 1, 0, 10159),
(@rid, 6682, 20, 6, '生产管理与技术创新', 1, 0, 10159),
(@rid, 6722, 15, 7, '培训与指导', 1, 0, 10159);

-- ============================================================
-- 执行后验证 SQL（可选，执行完上方 INSERT 后运行）
-- ============================================================
-- ① 每门课恰有一条 type=3 且 detail 合计=100
-- SELECT r.course_id, r.id, r.title, COUNT(d.id) AS items, SUM(d.rate) AS sum_rate
-- FROM exam_knowledge_rate r
-- JOIN exam_knowledge_rate_detail d ON d.knowledge_rate_id = r.id AND d.deleted = 0
-- WHERE r.type = 3 AND r.deleted = 0
-- GROUP BY r.course_id, r.id, r.title
-- ORDER BY r.course_id;
-- 预期：5 行，sum_rate 全部 = 100

-- ② 抽查 detail 挂载节点确为理论树 L1
-- SELECT d.knowledge_id, k.title, k.level, k.course_id
-- FROM exam_knowledge_rate_detail d
-- JOIN exam_knowledge_rate r ON r.id = d.knowledge_rate_id AND r.type = 3
-- JOIN exam_knowledge_structure k ON k.id = d.knowledge_id
-- WHERE d.deleted = 0 AND (k.level <> 1 OR k.deleted = 1);
-- 预期：0 行
