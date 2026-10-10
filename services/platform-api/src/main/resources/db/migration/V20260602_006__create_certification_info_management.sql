-- 信息管理：认定管理员账号的考务人员（考评员台账链路补充）
-- 本文件按生产库 flyway_schema_history(version=20260602.006, script=V20260602_006__create_certification_info_management.sql)
-- 与现网表结构回放重建（仓库中一度缺失该文件）。
-- 注意：后续 V20260605_010 同名迁移为条件化幂等版本（SET @var + PREPARE 动态 DDL），
-- 生产环境两份均已应用；本文件以"直接 DDL + INSERT IGNORE"口径重建，保持幂等。

-- 1) certification_info_exam_staff（考务人员台账；后续 V20260603_010/011 有结构演进，本表为初版骨架）
CREATE TABLE IF NOT EXISTS certification_info_exam_staff (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '绑定平台用户（认定管理员账号）',
  staff_name VARCHAR(80) NOT NULL COMMENT '姓名',
  phone VARCHAR(32) NOT NULL COMMENT '手机号',
  masked_phone VARCHAR(32) NULL COMMENT '脱敏手机号',
  account_username VARCHAR(80) NULL COMMENT '账号用户名',
  audit_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态 pending/approved/rejected',
  work_status VARCHAR(20) NOT NULL DEFAULT 'idle' COMMENT '工作状态 idle/assigned',
  referenced_count INT NOT NULL DEFAULT 0 COMMENT '被任务引用次数',
  submitted_at DATETIME NULL COMMENT '提交时间',
  submitted_by BIGINT NULL COMMENT '提交人',
  reviewed_at DATETIME NULL COMMENT '审核时间',
  reviewed_by BIGINT NULL COMMENT '审核人',
  reviewer_name VARCHAR(80) NULL COMMENT '审核人姓名',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1有效 0停用',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_cert_info_exam_staff_tenant (tenant_id, audit_status, work_status),
  KEY idx_cert_info_exam_staff_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='认定信息-考务人员台账';

-- 2) 信息管理子菜单（目录 6330 下的 6311/6312/6313；菜单目录本身由 V20260602_008 修复）
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6311, 10159, 6330, 'page', 'certification_info_assessor', '考评员信息',
    '/certification/info/assessor', 'certification/info/AssessorInfoPage',
    'certification.info.assessor.view', 'TeamOutlined', 1, 1, 'enabled',
    NOW(), NOW(), 1, 1, 0),
  (6312, 10159, 6330, 'page', 'certification_info_site', '考场信息',
    '/certification/info/site', 'certification/info/ExamSiteInfoPage',
    'certification.info.site.view', 'EnvironmentOutlined', 2, 1, 'enabled',
    NOW(), NOW(), 1, 1, 0),
  (6313, 10159, 6330, 'page', 'certification_info_equipment', '考试设备',
    '/certification/info/equipment', 'certification/info/ExamEquipmentInfoPage',
    'certification.info.equipment.view', 'ToolOutlined', 3, 1, 'enabled',
    NOW(), NOW(), 1, 1, 0);

-- 3) 权限码（三步链：sys_permission；role 授权由 V20260602_008 统一补齐）
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6961, 10159, 6311, 'certification.info.assessor.view', '查看考评员信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6962, 10159, 6311, 'certification.info.assessor.create', '新增考评员信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6963, 10159, 6311, 'certification.info.assessor.submit', '提交考评员审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6964, 10159, 6311, 'certification.info.assessor.review', '审核考评员信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6967, 10159, 6312, 'certification.info.site.view', '查看考场信息', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6968, 10159, 6312, 'certification.info.site.create', '新增考场信息', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6969, 10159, 6312, 'certification.info.site.submit', '提交考场审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6970, 10159, 6312, 'certification.info.site.review', '审核考场信息', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6974, 10159, 6313, 'certification.info.equipment.view', '查看考试设备', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6975, 10159, 6313, 'certification.info.equipment.create', '新增考试设备', 'create', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6976, 10159, 6313, 'certification.info.equipment.submit', '提交设备审核', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6977, 10159, 6313, 'certification.info.equipment.review', '审核考试设备', 'review', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 4) 平台管理员（role_id=2000）挂接菜单与权限
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806311, 10159, 2000, 6311, NOW(), NOW(), 1, 1, 0),
  (806312, 10159, 2000, 6312, NOW(), NOW(), 1, 1, 0),
  (806313, 10159, 2000, 6313, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  999200 + ROW_NUMBER() OVER (ORDER BY p.id),
  10159, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code LIKE 'certification.info.%'
  AND p.deleted = 0;
