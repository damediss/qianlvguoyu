CREATE TABLE IF NOT EXISTS certification_pre_exam_summary (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  material_imported_count INT NOT NULL DEFAULT 0 COMMENT '材料已导入数',
  material_total_count INT NOT NULL DEFAULT 0 COMMENT '材料总数',
  equipment_confirmed_count INT NOT NULL DEFAULT 0 COMMENT '设备已确认数',
  equipment_total_count INT NOT NULL DEFAULT 0 COMMENT '设备总数',
  equipment_unhandled_exception_count INT NOT NULL DEFAULT 0 COMMENT '未处理设备异常数',
  staff_completed_category_count INT NOT NULL DEFAULT 0 COMMENT '人员已办结类别数',
  staff_category_total_count INT NOT NULL DEFAULT 0 COMMENT '人员类别总数',
  staff_total_count INT NOT NULL DEFAULT 0 COMMENT '考务人员总人数',
  total_completed_count INT NOT NULL DEFAULT 0 COMMENT '总完成项数',
  total_item_count INT NOT NULL DEFAULT 0 COMMENT '总项数',
  completion_rate INT NOT NULL DEFAULT 0 COMMENT '完成率',
  material_review_status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '材料复核状态',
  equipment_review_status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '设备复核状态',
  staff_review_status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '人员复核状态',
  stage_status VARCHAR(40) NOT NULL DEFAULT 'in_progress' COMMENT '阶段状态',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  completed_at DATETIME NULL COMMENT '完成时间',
  completed_by BIGINT NULL COMMENT '完成人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_pre_exam_summary_task (tenant_id, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前任务摘要';

CREATE TABLE IF NOT EXISTS certification_pre_exam_template (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  item_type VARCHAR(40) NOT NULL COMMENT 'material/equipment/staff',
  item_code VARCHAR(40) NOT NULL COMMENT '模板编码',
  item_name VARCHAR(120) NOT NULL COMMENT '模板名称',
  required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必需',
  default_quantity INT NOT NULL DEFAULT 1 COMMENT '默认数量',
  require_file TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须上传附件',
  unit VARCHAR(20) NULL COMMENT '单位',
  description VARCHAR(500) NULL COMMENT '说明',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  sort_order INT NOT NULL DEFAULT 100 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_pre_exam_template_code (tenant_id, item_type, item_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前清单模板';

CREATE TABLE IF NOT EXISTS certification_exam_material_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  template_id BIGINT NULL COMMENT '模板id',
  item_code VARCHAR(40) NOT NULL COMMENT '材料编码',
  item_name VARCHAR(120) NOT NULL COMMENT '材料名称',
  required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必需',
  require_file TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必须上传附件',
  expected_quantity INT NOT NULL DEFAULT 1 COMMENT '应备数量',
  actual_quantity INT NOT NULL DEFAULT 0 COMMENT '实备数量',
  unit VARCHAR(20) NULL COMMENT '单位',
  responsible_person VARCHAR(80) NULL COMMENT '责任人',
  file_id BIGINT NULL COMMENT '附件文件id',
  file_name VARCHAR(300) NULL COMMENT '附件文件名',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  status VARCHAR(40) NOT NULL DEFAULT 'missing' COMMENT '状态',
  imported_by_name VARCHAR(80) NULL COMMENT '导入人',
  imported_at DATETIME NULL COMMENT '导入时间',
  reviewed_by_name VARCHAR(80) NULL COMMENT '复核人',
  reviewed_at DATETIME NULL COMMENT '复核时间',
  review_comment VARCHAR(500) NULL COMMENT '复核意见',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_exam_material_task_code (tenant_id, task_id, item_code, deleted),
  KEY idx_cert_exam_material_task (tenant_id, task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前考试材料清单';

CREATE TABLE IF NOT EXISTS certification_exam_equipment_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  template_id BIGINT NULL COMMENT '模板id',
  item_code VARCHAR(40) NOT NULL COMMENT '设备编码',
  item_name VARCHAR(120) NOT NULL COMMENT '设备名称',
  exam_site_id BIGINT NULL COMMENT '所属考场id',
  exam_site_name VARCHAR(200) NULL COMMENT '所属考场',
  model_spec VARCHAR(200) NULL COMMENT '型号规格',
  required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必需',
  expected_quantity INT NOT NULL DEFAULT 1 COMMENT '应备数量',
  actual_quantity INT NOT NULL DEFAULT 0 COMMENT '实备数量',
  unit VARCHAR(20) NULL COMMENT '单位',
  responsible_person VARCHAR(80) NULL COMMENT '责任人',
  status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '状态',
  check_result VARCHAR(80) NULL COMMENT '检查结果',
  exception_description VARCHAR(500) NULL COMMENT '异常说明',
  exception_status VARCHAR(40) NOT NULL DEFAULT 'none' COMMENT '异常处理状态',
  checked_by_name VARCHAR(80) NULL COMMENT '检查人',
  checked_at DATETIME NULL COMMENT '检查时间',
  attachment_file_id BIGINT NULL COMMENT '附件id',
  attachment_file_name VARCHAR(300) NULL COMMENT '附件名称',
  review_comment VARCHAR(500) NULL COMMENT '复核意见',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_exam_equipment_task_code (tenant_id, task_id, item_code, deleted),
  KEY idx_cert_exam_equipment_task (tenant_id, task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前考试设备清单';

CREATE TABLE IF NOT EXISTS certification_exam_staff_category (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  template_id BIGINT NULL COMMENT '模板id',
  category_code VARCHAR(40) NOT NULL COMMENT '类别编码',
  category_name VARCHAR(80) NOT NULL COMMENT '类别名称',
  required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必需',
  minimum_count INT NOT NULL DEFAULT 1 COMMENT '最低人数',
  current_count INT NOT NULL DEFAULT 0 COMMENT '当前有效人数',
  complete_status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '办结状态',
  completed_by_name VARCHAR(80) NULL COMMENT '办结人',
  completed_at DATETIME NULL COMMENT '办结时间',
  review_status VARCHAR(40) NOT NULL DEFAULT 'pending' COMMENT '复核状态',
  reviewed_by_name VARCHAR(80) NULL COMMENT '复核人',
  reviewed_at DATETIME NULL COMMENT '复核时间',
  review_comment VARCHAR(500) NULL COMMENT '复核意见',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_exam_staff_category_code (tenant_id, task_id, category_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前考务人员类别';

CREATE TABLE IF NOT EXISTS certification_exam_staff (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  category_id BIGINT NOT NULL COMMENT '类别id',
  category_code VARCHAR(40) NOT NULL COMMENT '类别编码',
  category_name VARCHAR(80) NOT NULL COMMENT '类别名称',
  staff_name VARCHAR(80) NOT NULL COMMENT '姓名',
  id_card_no VARCHAR(32) NOT NULL COMMENT '证件号码',
  masked_id_card_no VARCHAR(32) NULL COMMENT '脱敏证件号码',
  phone VARCHAR(32) NOT NULL COMMENT '联系电话',
  masked_phone VARCHAR(32) NULL COMMENT '脱敏联系电话',
  org_name VARCHAR(160) NOT NULL COMMENT '单位部门',
  duty VARCHAR(120) NULL COMMENT '职责岗位',
  exam_site_id BIGINT NULL COMMENT '负责考场id',
  exam_site_name VARCHAR(200) NULL COMMENT '负责考场',
  user_id BIGINT NULL COMMENT '平台用户id',
  assessor_id BIGINT NULL COMMENT '考评员档案id',
  task_assessor_id BIGINT NULL COMMENT '任务考评员分配id',
  certificate_no VARCHAR(120) NULL COMMENT '资质证书号',
  work_type_scope VARCHAR(500) NULL COMMENT '工种范围',
  status VARCHAR(40) NOT NULL DEFAULT 'active' COMMENT '状态',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_exam_staff_task (tenant_id, task_id, category_code, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前考务人员明细';

CREATE TABLE IF NOT EXISTS certification_pre_exam_review_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户id',
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  review_type VARCHAR(40) NOT NULL COMMENT 'material/equipment/staff/stage',
  action VARCHAR(40) NOT NULL COMMENT 'pass/reject/cancel/complete',
  result_status VARCHAR(40) NOT NULL COMMENT '结果状态',
  review_comment VARCHAR(500) NULL COMMENT '意见',
  operator_name VARCHAR(80) NULL COMMENT '操作人',
  operated_at DATETIME NOT NULL COMMENT '操作时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_pre_exam_review_task (tenant_id, task_id, review_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认定前复核记录';

INSERT IGNORE INTO certification_pre_exam_template (
  id, tenant_id, item_type, item_code, item_name, required, default_quantity, require_file, unit, description, enabled, sort_order,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (720001, 1, 'material', 'MAT001', '考试通知单', 1, 1, 1, '份', NULL, 1, 1, NOW(), NOW(), 1, 1, 0),
  (720002, 1, 'material', 'MAT002', '考生签到表', 1, 1, 1, '份', NULL, 1, 2, NOW(), NOW(), 1, 1, 0),
  (720003, 1, 'material', 'MAT003', '考场座位表', 1, 1, 1, '份', NULL, 1, 3, NOW(), NOW(), 1, 1, 0),
  (720004, 1, 'material', 'MAT004', '考试试卷（理论）', 1, 1, 1, '份', NULL, 1, 4, NOW(), NOW(), 1, 1, 0),
  (720005, 1, 'material', 'MAT005', '考试试卷（实操）', 1, 1, 1, '份', NULL, 1, 5, NOW(), NOW(), 1, 1, 0),
  (720006, 1, 'material', 'MAT006', '评分标准表', 1, 1, 1, '份', NULL, 1, 6, NOW(), NOW(), 1, 1, 0),
  (720007, 1, 'material', 'MAT007', '考场规则告示', 1, 1, 1, '份', NULL, 1, 7, NOW(), NOW(), 1, 1, 0),
  (720008, 1, 'material', 'MAT008', '应急预案文档', 1, 1, 1, '份', NULL, 1, 8, NOW(), NOW(), 1, 1, 0),
  (720101, 1, 'equipment', 'EQUIP001', '考试用计算机', 1, 1, 0, '台', NULL, 1, 101, NOW(), NOW(), 1, 1, 0),
  (720102, 1, 'equipment', 'EQUIP002', '实操考试设备', 1, 1, 0, '套', NULL, 1, 102, NOW(), NOW(), 1, 1, 0),
  (720103, 1, 'equipment', 'EQUIP003', '监控设备', 1, 1, 0, '套', NULL, 1, 103, NOW(), NOW(), 1, 1, 0),
  (720104, 1, 'equipment', 'EQUIP004', '计时器/时钟', 1, 1, 0, '个', NULL, 1, 104, NOW(), NOW(), 1, 1, 0),
  (720105, 1, 'equipment', 'EQUIP005', '广播/音响设备', 1, 1, 0, '套', NULL, 1, 105, NOW(), NOW(), 1, 1, 0),
  (720106, 1, 'equipment', 'EQUIP006', '身份核验设备', 1, 1, 0, '台', NULL, 1, 106, NOW(), NOW(), 1, 1, 0),
  (720107, 1, 'equipment', 'EQUIP007', '网络及电力保障', 1, 1, 0, '项', NULL, 1, 107, NOW(), NOW(), 1, 1, 0),
  (720108, 1, 'equipment', 'EQUIP008', '应急备用设备', 1, 1, 0, '套', NULL, 1, 108, NOW(), NOW(), 1, 1, 0),
  (720201, 1, 'staff', 'assessor', '考评员', 1, 1, 0, '人', '优先从筹备期任务考评员分配带入', 1, 201, NOW(), NOW(), 1, 1, 0),
  (720202, 1, 'staff', 'supervisor', '督导员', 1, 1, 0, '人', '负责质量督导与过程监督', 1, 202, NOW(), NOW(), 1, 1, 0),
  (720203, 1, 'staff', 'invigilator', '监考员', 1, 1, 0, '人', '负责考场秩序与考纪监督', 1, 203, NOW(), NOW(), 1, 1, 0),
  (720204, 1, 'staff', 'exam_staff', '考务人员', 1, 1, 0, '人', '负责资料、签到、现场协调等工作', 1, 204, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6320, 1, 6300, 'page', 'certification_pre_exam', '认定前', '/certification/pre-exam', 'certification/PreExam', 'certification.pre_exam.view', 'SafetyCertificateOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6950, 1, 6320, 'certification.pre_exam.view', '查看认定前', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6951, 1, 6320, 'certification.pre_exam.material.edit', '编辑认定前材料', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6952, 1, 6320, 'certification.pre_exam.material.review', '复核认定前材料', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6953, 1, 6320, 'certification.pre_exam.equipment.edit', '编辑认定前设备', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6954, 1, 6320, 'certification.pre_exam.equipment.review', '复核认定前设备', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6955, 1, 6320, 'certification.pre_exam.staff.edit', '编辑认定前人员', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6956, 1, 6320, 'certification.pre_exam.staff.review', '复核认定前人员', 'review', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6957, 1, 6320, 'certification.pre_exam.complete', '完成认定前阶段', 'complete', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 909500 + p.id - 6950, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.pre_exam.view',
    'certification.pre_exam.material.edit',
    'certification.pre_exam.material.review',
    'certification.pre_exam.equipment.edit',
    'certification.pre_exam.equipment.review',
    'certification.pre_exam.staff.edit',
    'certification.pre_exam.staff.review',
    'certification.pre_exam.complete'
  );
