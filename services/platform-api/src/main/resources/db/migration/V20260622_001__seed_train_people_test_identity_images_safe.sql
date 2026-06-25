-- 为培训学员测试数据补充可复现的上游身份证图片来源。
-- V20260611_003 已移除 train_people_list.id_card_front / id_card_back，
-- 且 Druid WallFilter 禁止 Flyway 执行 PREPARE 动态 SQL，此处保留幂等占位。
SELECT 1;
