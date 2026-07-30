-- 认定筹备：任务考务配置（绑定信息管理中的考务人员）
CREATE TABLE IF NOT EXISTS certification_task_exam_staff (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  staff_id BIGINT NULL COMMENT '考务人员档案id',
  staff_name VARCHAR(80) NOT NULL COMMENT '考务人员姓名',
  phone VARCHAR(32) NULL COMMENT '联系电话',
  masked_phone VARCHAR(32) NULL COMMENT '脱敏电话',
  remark VARCHAR(500) NULL COMMENT '备注',
  status VARCHAR(40) NOT NULL DEFAULT 'configured' COMMENT '配置状态',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_task_exam_staff (tenant_id, task_id, staff_id),
  KEY idx_cert_task_exam_staff_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务考务配置';

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6997, 1, 6310, 'certification.preparation.staff.view', '查看考务配置', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6998, 1, 6310, 'certification.preparation.staff.assign', '分配考务人员', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9072000 + rp.role_id * 10 + (p.id - 6996), rp.tenant_id, rp.role_id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
CROSS JOIN sys_permission p
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.permission_id IN (6933, 6935, 6921)
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND p.id IN (6997, 6998);
