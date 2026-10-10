-- ===========================================================================
-- 培训管理 - 培训需求 测试数据
-- 日期: 2026-06-05
--
-- 目标表: demand_train（企业培训需求）
-- 租户: id=1 (demo)
-- 企业: 演示公司(100) + 新增 2 家测试企业
-- 兼容共享库（created_at）与 Flyway 新建库（create_time / addtime）
-- ===========================================================================

SET @db := DATABASE();

-- 共享库可能缺少 addtime，补齐后待办列表可按时间排序
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'addtime') = 0,
  'ALTER TABLE demand_train ADD COLUMN addtime INT NULL COMMENT ''添加时间戳'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补充测试企业（供需求下拉与列表展示）
INSERT IGNORE INTO sys_org (
  id, tenant_id, parent_id, org_type, code, name, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (101, 1, 0, 'company', 'test-gxgy', '广西铝业集团有限公司', 2, 'enabled', NOW(), NOW(), 1, 1, 0),
  (102, 1, 0, 'company', 'test-qianlv', '千铝工投科技有限公司', 3, 'enabled', NOW(), NOW(), 1, 1, 0);

SET @has_created_at := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'created_at'
);
SET @has_create_time := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'create_time'
);

-- 共享库：created_at / updated_at / created_by / updated_by
SET @sql := IF(
  @has_created_at > 0,
  'INSERT IGNORE INTO demand_train (id, tenant_id, org_id, content, nums, status, addtime, created_at, updated_at, created_by, updated_by, deleted) VALUES
    (10001, 1, 100, ''电工初级工培训：企业新入职电工 30 人，需开展五级/初级工职业技能等级认定培训，含理论+实操。'', 30, 0, UNIX_TIMESTAMP(''2026-06-01 09:15:00''), ''2026-06-01 09:15:00'', NOW(), 1, 1, 0),
    (10002, 1, 101, ''焊工中级工培训：生产一线焊工 25 人，申请四级/中级工培训，重点加强氩弧焊与 CO2 气体保护焊技能。'', 25, 0, UNIX_TIMESTAMP(''2026-06-02 10:30:00''), ''2026-06-02 10:30:00'', NOW(), 1, 1, 0),
    (10003, 1, 102, ''钳工高级工培训：设备维修班组 18 人，需三级/高级工培训，要求配备高级技师授课并安排企业现场实操。'', 18, 0, UNIX_TIMESTAMP(''2026-06-03 14:00:00''), ''2026-06-03 14:00:00'', NOW(), 1, 1, 0),
    (10004, 1, 100, ''起重机械操作工培训：仓储物流岗位 12 人，申请桥式起重机操作工初级培训及取证辅导。'', 12, 0, UNIX_TIMESTAMP(''2026-06-04 08:45:00''), ''2026-06-04 08:45:00'', NOW(), 1, 1, 0),
    (10005, 1, 101, ''数控车工技师培训：数控加工中心骨干 8 人，申请二级/技师培训，需定制企业产线案例教学方案。'', 8, 0, UNIX_TIMESTAMP(''2026-06-05 11:20:00''), ''2026-06-05 11:20:00'', NOW(), 1, 1, 0),
    (10006, 1, 102, ''安全管理员培训：各车间安全员 15 人，已完成立项并转入培训信息阶段（已处理示例）。'', 15, 1, UNIX_TIMESTAMP(''2026-05-28 16:00:00''), ''2026-05-28 16:00:00'', NOW(), 1, 1, 0)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Flyway 新建库：create_time / update_time
SET @sql := IF(
  @has_created_at = 0 AND @has_create_time > 0,
  'INSERT IGNORE INTO demand_train (id, tenant_id, org_id, content, nums, status, addtime, create_time, update_time, deleted) VALUES
    (10001, 1, 100, ''电工初级工培训：企业新入职电工 30 人，需开展五级/初级工职业技能等级认定培训，含理论+实操。'', 30, 0, UNIX_TIMESTAMP(''2026-06-01 09:15:00''), ''2026-06-01 09:15:00'', NOW(), 0),
    (10002, 1, 101, ''焊工中级工培训：生产一线焊工 25 人，申请四级/中级工培训，重点加强氩弧焊与 CO2 气体保护焊技能。'', 25, 0, UNIX_TIMESTAMP(''2026-06-02 10:30:00''), ''2026-06-02 10:30:00'', NOW(), 0),
    (10003, 1, 102, ''钳工高级工培训：设备维修班组 18 人，需三级/高级工培训，要求配备高级技师授课并安排企业现场实操。'', 18, 0, UNIX_TIMESTAMP(''2026-06-03 14:00:00''), ''2026-06-03 14:00:00'', NOW(), 0),
    (10004, 1, 100, ''起重机械操作工培训：仓储物流岗位 12 人，申请桥式起重机操作工初级培训及取证辅导。'', 12, 0, UNIX_TIMESTAMP(''2026-06-04 08:45:00''), ''2026-06-04 08:45:00'', NOW(), 0),
    (10005, 1, 101, ''数控车工技师培训：数控加工中心骨干 8 人，申请二级/技师培训，需定制企业产线案例教学方案。'', 8, 0, UNIX_TIMESTAMP(''2026-06-05 11:20:00''), ''2026-06-05 11:20:00'', NOW(), 0),
    (10006, 1, 102, ''安全管理员培训：各车间安全员 15 人，已完成立项并转入培训信息阶段（已处理示例）。'', 15, 1, UNIX_TIMESTAMP(''2026-05-28 16:00:00''), ''2026-05-28 16:00:00'', NOW(), 0)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
