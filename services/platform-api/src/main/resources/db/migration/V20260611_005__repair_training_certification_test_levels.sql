-- 修复认定上游链路测试数据的等级编码。
-- 培训项目/班级使用 1-5（5=初级），报名人员沿用旧系统 0-4（0=初级）。

UPDATE train_info_list
SET work_type_level = 5
WHERE id = 20008
  AND tenant_id = 1
  AND title = '认定上游链路测试培训班'
  AND deleted = 0;

UPDATE train_info_class
SET work_type_level = 5
WHERE id = 30009
  AND train_info_id = 20008
  AND title = '认定上游链路测试一班'
  AND deleted = 0;

UPDATE train_sign_up_people_self
SET req_level = 0,
    level = '0',
    work_type_paper_level = 0,
    update_time = NOW()
WHERE id BETWEEN 91011 AND 91015
  AND sign_up_id = 90008
  AND tenant_id = 1
  AND deleted = 0;
