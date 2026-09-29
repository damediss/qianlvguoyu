-- 模拟培训业务完整链路：
-- 培训项目/班级 -> 信息收集任务 -> 5 条报名资料 -> 业务人员导入班级学员。
-- 完整资料保存在 train_sign_up_people_self，train_people_list 仅保存班级成员关系和基础身份。

INSERT IGNORE INTO train_info_list (
  id, title, company_id, company_name, company_address,
  work_type_id, work_type_level, num, position,
  start_date, end_date, train_company_name,
  status, sort_number, tenant_id, create_time, deleted
) VALUES (
  20008, '认定上游链路测试培训班', 100, '演示公司', '广西南宁市测试路 88 号',
  28, 4, 5, '焊接操作岗',
  '2026-06-01', '2026-08-31', '演示培训机构',
  1, 100, 1, '2026-06-11 10:00:00', 0
);

INSERT IGNORE INTO train_info_class (
  id, train_info_id, title, nums, company_id, work_type_id, work_type_level,
  date_start, date_end, addtime, deleted
) VALUES (
  30009, 20008, '认定上游链路测试一班', 5, 100, 28, 4,
  '2026-06-01', '2026-08-31', UNIX_TIMESTAMP('2026-06-11 10:05:00'), 0
);

INSERT IGNORE INTO train_sign_up_self (
  id, title, train_time, exam_time, org_id, train_info_id,
  sort_number, tenant_id, create_time, update_time, deleted
) VALUES (
  90008, '认定上游链路测试信息收集', '2026-06-01 至 2026-08-31', '2026-09-15',
  100, 20008, 100, 1, '2026-06-11 10:10:00', '2026-06-11 10:10:00', 0
);

-- 空库幂等化（踩坑 #34 修复）：原语句按 PHP 老表 ~50 个 legacy 列插入，链上建表
-- （V20260521_001）只有精简列，空库必报 Unknown column。重写为精简列版本，保留
-- 演示链路要素（5 名学员挂在 90008 收集任务下）；存量库本版本已应用不会重跑。
INSERT INTO train_sign_up_people_self (
  id, sign_up_id, platform_user_id, name, phone, id_card_number, gender, education,
  status, tenant_id, create_time, update_time, deleted
) VALUES
  (91011, 90008, 990001, '测试学员甲', '13900009101', '450102199001011012', '男', '大专', 1, 1, '2026-06-11 10:11:00', '2026-06-11 10:11:00', 0),
  (91012, 90008, 990002, '测试学员乙', '13900009102', '450102199202022025', '女', '大专', 1, 1, '2026-06-11 10:12:00', '2026-06-11 10:12:00', 0),
  (91013, 90008, 990003, '测试学员丙', '13900009103', '450102199403033038', '男', '大专', 1, 1, '2026-06-11 10:13:00', '2026-06-11 10:13:00', 0),
  (91014, 90008, 990004, '测试学员丁', '13900009104', '450102199604044040', '女', '大专', 1, 1, '2026-06-11 10:14:00', '2026-06-11 10:14:00', 0),
  (91015, 90008, 990005, '测试学员戊', '13900009105', '450102199805055053', '男', '大专', 1, 1, '2026-06-11 10:15:00', '2026-06-11 10:15:00', 0)
ON DUPLICATE KEY UPDATE
  sign_up_id = VALUES(sign_up_id),
  name = VALUES(name),
  phone = VALUES(phone),
  platform_user_id = VALUES(platform_user_id),
  id_card_number = VALUES(id_card_number),
  deleted = 0,
  update_time = VALUES(update_time);

-- 等价于业务人员在班级学员页面选择上述报名人员并点击“导入”。
INSERT INTO train_people_list (
  train_id, train_class_id, type, user_id, platform_user_id,
  realname, phone, is_leader, is_committee, sort_number,
  tenant_id, create_time, id_card, deleted
)
SELECT
  20008, 30009, 'student', source.platform_user_id, source.platform_user_id,
  source.name, source.phone, 0, 0, 0,
  source.tenant_id, '2026-06-11 10:20:00', source.id_card_number, 0
FROM train_sign_up_people_self source
WHERE source.id BETWEEN 91011 AND 91015
  AND source.sign_up_id = 90008
  AND source.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM train_people_list student
    WHERE student.train_id = 20008
      AND student.train_class_id = 30009
      AND student.type = 'student'
      AND student.platform_user_id = source.platform_user_id
      AND student.deleted = 0
  );
