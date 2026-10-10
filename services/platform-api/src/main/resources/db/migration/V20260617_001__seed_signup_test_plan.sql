-- ============================================================
-- 培训报名·扫码信息收集测试 - 注入一条可用的报名计划
-- ============================================================
INSERT INTO train_sign_up_self (id, title, train_time, exam_time, org_id, train_info_id, sort_number, tenant_id, deleted)
SELECT 9999,
       '【测试】扫码信息收集演示计划',
       '2026-07-01 ~ 2026-07-15',
       '2026-07-20 09:00',
       1, 1, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM train_sign_up_self WHERE id = 9999);
