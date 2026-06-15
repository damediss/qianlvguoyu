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

INSERT INTO train_sign_up_people_self (
  id, sign_up_id, name, age, req_work_type_id, req_work_type_name, req_level,
  job, work_type_id, work_type_name, level,
  company_id, company_name, company_code, zip_code, company_address, industrial_park,
  phone, user_id, platform_user_id, comments, company_status, status, sort_number,
  tenant_id, create_time, update_time, deleted,
  id_card_front, id_card_back, id_card_number, gender,
  work_type_paper_has, work_type_paper_no, work_type_paper_level, work_type_paper_name,
  work_date_start, work_time, title_type, title_level, has_pay,
  school, major, in_school_date, graduate_date, school_paper_has,
  nation, address, education, `condition`, school_no, paper_no, work_exp,
  house_type, political, task, jog
) VALUES
  (91011, 90008, '测试学员甲', 36, 28, '焊工', 4,
   '焊接操作员', 28, '焊工', 5,
   100, '演示公司', '91450100TEST00001', '530000', '广西南宁市测试路 88 号', '南宁测试工业园',
   '13900009101', 99101, 990001, '认定完整链路测试数据 1', 1, 1, 1,
   1, '2026-06-11 10:11:00', '2026-06-11 10:11:00', 0,
   'classpath:/test-data/certification/test-id-card-front.png',
   'classpath:/test-data/certification/test-id-card-back.png',
   '450102199001011012', '男',
   1, 'TEST-WELDER-001', 5, '焊工',
   '2014-07-01', '12年', '助理工程师', '初级', 1,
   '广西测试职业技术学院', '焊接技术', '2008-09-01', '2011-06-30', 1,
   '壮族', '广西南宁市青秀区测试街 1 号', '大专',
   '取得初级工证书后累计从事本职业工作满4年',
   'TEST-DIPLOMA-001', 'TEST-CERT-001',
   '[{"company":"演示公司","position":"焊接操作员","start":"2014-07","end":"至今"}]',
   '城镇', '群众', 1, '焊接操作岗'),

  (91012, 90008, '测试学员乙', 34, 28, '焊工', 4,
   '焊接质检员', 28, '焊工', 5,
   100, '演示公司', '91450100TEST00001', '530000', '广西南宁市测试路 88 号', '南宁测试工业园',
   '13900009102', 99102, 990002, '认定完整链路测试数据 2', 1, 1, 2,
   1, '2026-06-11 10:12:00', '2026-06-11 10:12:00', 0,
   'classpath:/test-data/certification/test-id-card-front.png',
   'classpath:/test-data/certification/test-id-card-back.png',
   '450102199202022025', '女',
   1, 'TEST-WELDER-002', 5, '焊工',
   '2015-08-01', '10年', '技术员', '初级', 1,
   '南宁测试技师学院', '材料成型', '2009-09-01', '2012-06-30', 1,
   '汉族', '广西南宁市西乡塘区测试街 2 号', '大专',
   '取得初级工证书后累计从事本职业工作满4年',
   'TEST-DIPLOMA-002', 'TEST-CERT-002',
   '[{"company":"演示公司","position":"焊接质检员","start":"2015-08","end":"至今"}]',
   '城镇', '群众', 1, '质量检查岗'),

  (91013, 90008, '测试学员丙', 32, 28, '焊工', 4,
   '设备检修员', 28, '焊工', 5,
   100, '演示公司', '91450100TEST00001', '530000', '广西南宁市测试路 88 号', '南宁测试工业园',
   '13900009103', 99103, 990003, '认定完整链路测试数据 3', 1, 1, 3,
   1, '2026-06-11 10:13:00', '2026-06-11 10:13:00', 0,
   'classpath:/test-data/certification/test-id-card-front.png',
   'classpath:/test-data/certification/test-id-card-back.png',
   '450102199403033038', '男',
   1, 'TEST-WELDER-003', 5, '焊工',
   '2016-09-01', '9年', '技术员', '初级', 1,
   '广西测试机电职业学院', '机电设备维修', '2010-09-01', '2013-06-30', 1,
   '汉族', '广西南宁市江南区测试街 3 号', '大专',
   '取得初级工证书后累计从事本职业工作满4年',
   'TEST-DIPLOMA-003', 'TEST-CERT-003',
   '[{"company":"演示公司","position":"设备检修员","start":"2016-09","end":"至今"}]',
   '农村', '群众', 1, '设备检修岗'),

  (91014, 90008, '测试学员丁', 30, 28, '焊工', 4,
   '生产操作员', 28, '焊工', 5,
   100, '演示公司', '91450100TEST00001', '530000', '广西南宁市测试路 88 号', '南宁测试工业园',
   '13900009104', 99104, 990004, '认定完整链路测试数据 4', 1, 1, 4,
   1, '2026-06-11 10:14:00', '2026-06-11 10:14:00', 0,
   'classpath:/test-data/certification/test-id-card-front.png',
   'classpath:/test-data/certification/test-id-card-back.png',
   '450102199604044040', '女',
   1, 'TEST-WELDER-004', 5, '焊工',
   '2017-07-01', '8年', '技术员', '初级', 1,
   '广西测试工业职业学院', '智能制造', '2011-09-01', '2014-06-30', 1,
   '壮族', '广西南宁市良庆区测试街 4 号', '大专',
   '取得初级工证书后累计从事本职业工作满4年',
   'TEST-DIPLOMA-004', 'TEST-CERT-004',
   '[{"company":"演示公司","position":"生产操作员","start":"2017-07","end":"至今"}]',
   '城镇', '共青团员', 1, '生产操作岗'),

  (91015, 90008, '测试学员戊', 28, 28, '焊工', 4,
   '安全巡检员', 28, '焊工', 5,
   100, '演示公司', '91450100TEST00001', '530000', '广西南宁市测试路 88 号', '南宁测试工业园',
   '13900009105', 99105, 990005, '认定完整链路测试数据 5', 1, 1, 5,
   1, '2026-06-11 10:15:00', '2026-06-11 10:15:00', 0,
   'classpath:/test-data/certification/test-id-card-front.png',
   'classpath:/test-data/certification/test-id-card-back.png',
   '450102199805055053', '男',
   1, 'TEST-WELDER-005', 5, '焊工',
   '2018-08-01', '7年', '技术员', '初级', 1,
   '南宁测试职业学院', '安全技术管理', '2012-09-01', '2015-06-30', 1,
   '汉族', '广西南宁市兴宁区测试街 5 号', '大专',
   '取得初级工证书后累计从事本职业工作满4年',
   'TEST-DIPLOMA-005', 'TEST-CERT-005',
   '[{"company":"演示公司","position":"安全巡检员","start":"2018-08","end":"至今"}]',
   '农村', '群众', 1, '安全巡检岗')
ON DUPLICATE KEY UPDATE
  sign_up_id = VALUES(sign_up_id),
  name = VALUES(name),
  phone = VALUES(phone),
  platform_user_id = VALUES(platform_user_id),
  id_card_number = VALUES(id_card_number),
  id_card_front = VALUES(id_card_front),
  id_card_back = VALUES(id_card_back),
  req_work_type_id = VALUES(req_work_type_id),
  req_work_type_name = VALUES(req_work_type_name),
  req_level = VALUES(req_level),
  company_name = VALUES(company_name),
  deleted = 0,
  update_time = VALUES(update_time);

-- 等价于业务人员在班级学员页面选择上述报名人员并点击“导入”。
INSERT INTO train_people_list (
  train_id, train_class_id, type, user_id, platform_user_id,
  realname, phone, is_leader, is_committee, sort_number,
  tenant_id, create_time, id_card, deleted
)
SELECT
  20008, 30009, 'student', source.user_id, source.platform_user_id,
  source.name, source.phone, 0, 0, source.sort_number,
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
