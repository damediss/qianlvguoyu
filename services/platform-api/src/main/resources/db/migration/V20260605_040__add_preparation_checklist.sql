CREATE TABLE IF NOT EXISTS certification_task_checklist_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  item_no INT NOT NULL COMMENT '序号',
  item_name VARCHAR(200) NOT NULL COMMENT '名称',
  quantity_text VARCHAR(80) NULL COMMENT '数量',
  checked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已核',
  confirmer_task_staff_id BIGINT NULL COMMENT '确认人任务考务id',
  confirmer_name VARCHAR(80) NULL COMMENT '确认人姓名',
  confirmed_at DATETIME NULL COMMENT '确认时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_task_checklist_item (tenant_id, task_id, item_no),
  KEY idx_cert_task_checklist_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定筹备清单核查';

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (7604, 1, 6310, 'certification.preparation.checklist.view', '查看清单核查', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7605, 1, 6310, 'certification.preparation.checklist.confirm', '确认清单核查', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9076000 + rp.role_id * 10 + (p.id - 7603), rp.tenant_id, rp.role_id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
CROSS JOIN sys_permission p
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.permission_id IN (6921, 6997, 6998)
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND p.id IN (7604, 7605);
