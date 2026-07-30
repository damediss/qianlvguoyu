UPDATE sys_menu
SET name = '信息采集', updated_at = NOW()
WHERE id = 6380
  AND code = 'certification_practical_collection';

UPDATE sys_permission
SET name = CASE code
  WHEN 'certification.practical.collection.view' THEN '查看信息采集'
  WHEN 'certification.practical.collection.operate' THEN '操作信息采集'
  WHEN 'certification.practical.collection.manage' THEN '管理信息采集'
  WHEN 'certification.practical.collection.exception' THEN '处理信息采集异常'
  WHEN 'certification.practical.collection.export' THEN '导出信息采集二维码'
  ELSE name
END,
updated_at = NOW()
WHERE code LIKE 'certification.practical.collection.%';
