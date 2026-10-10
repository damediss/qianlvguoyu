-- 空库幂等化（踩坑 #34 修复）：存量库 certification_exam_equipment 是 PHP 老表已存在；
-- 空库中该表由下游 V20260605_010 才建出（该建表已补上 equipment_quantity 列），
-- 此处表不存在即跳过；存量库本版本已应用不会重跑（validate-on-migrate=false）。
SET @db := DATABASE();
SET @tbl := (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_exam_equipment');

SET @sql := IF(
  @tbl > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_exam_equipment'
      AND COLUMN_NAME = 'equipment_quantity') = 0,
  'ALTER TABLE certification_exam_equipment ADD COLUMN equipment_quantity INT NOT NULL DEFAULT 1 COMMENT ''设备数量'' AFTER asset_no',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @tbl > 0,
  'UPDATE certification_exam_equipment SET equipment_status = ''idle'' WHERE equipment_status = ''normal'' AND deleted = 0',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS certification_task_equipment (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  equipment_id BIGINT NOT NULL COMMENT '设备档案id',
  equipment_name VARCHAR(120) NOT NULL COMMENT '设备名',
  equipment_no VARCHAR(120) NOT NULL COMMENT '设备编号',
  equipment_quantity INT NOT NULL DEFAULT 1 COMMENT '设备数量',
  status VARCHAR(40) NOT NULL DEFAULT 'configured' COMMENT '配置状态',
  remark VARCHAR(500) NULL COMMENT '备注',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_task_equipment (tenant_id, task_id, equipment_id, deleted),
  KEY idx_cert_task_equipment_task (tenant_id, task_id, deleted),
  KEY idx_cert_task_equipment_equipment (tenant_id, equipment_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定筹备任务设备配置';

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (7001, 1, 6310, 'certification.preparation.equipment.view', '查看配置设备', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7002, 1, 6310, 'certification.preparation.equipment.assign', '配置任务设备', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9073000 + rp.role_id * 10 + (p.id - 7000), rp.tenant_id, rp.role_id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
CROSS JOIN sys_permission p
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.permission_id IN (6933, 6935, 6921)
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND p.id IN (7001, 7002);
