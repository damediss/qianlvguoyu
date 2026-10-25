-- 职业标准驱动教材生成（二期编写协同）：5 张新表
-- 方案：docs/04_requirements/职业标准驱动教材生成_二期编写协同实施设计.md v1.1 §5.1~§5.5
-- 取号（2026-10-24 双库核查）：测试库 gjjddb2026（114.132.67.185:3309）MAX(version)=20261022.001，
-- 已真实查询确认；生产库 gjjddbpro（43.138.163.7:3308）以 gjjd + 测试库同密码只读连接被拒
-- （ERROR 1045 Access denied，2026-10-24 实测），无法核实 MAX(version)。按 AGENTS.md 间隔口径
-- 不回填、取当日新号 20261024.001（高于测试库最大值与共用目录全部既有号；生产库共用目录
-- 已应用最大号按仓库文件为 20261021.004，本号不与其冲突；若生产库存在他人 20261022~20261024
-- 期间的未入仓迁移，需在上线前补查双库确认）。
-- 幂等：CREATE TABLE IF NOT EXISTS，空库可全量跑通；BaseEntity 六列齐全（tenant_id/deleted/
-- created_by/updated_by/created_at/updated_at）。

-- ============================================================
-- 1) textbook_chapter_assignment 章当前分配 + 查询进度缓存
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_chapter_assignment (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '教材工程 ID（冗余便于按工程查询）',
  chapter_id BIGINT NOT NULL COMMENT 'textbook_outline.id，必须章级（node_level=2）',
  writer_id BIGINT NOT NULL COMMENT '主笔 sys_user.id',
  reviewer_id BIGINT NOT NULL COMMENT '审定 sys_user.id，与主笔不同人',
  due_date DATE NULL COMMENT '截止日期（NULL=不限）',
  instruction VARCHAR(500) NULL COMMENT '编写说明',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '1 有效，0 已取消；不是 deleted',
  task_status VARCHAR(16) NOT NULL DEFAULT 'assigned' COMMENT 'assigned/writing/reviewing/returned/approved（进度缓存，事实源是节正文状态）',
  section_count INT NOT NULL DEFAULT 0 COMMENT '章下存活节总数缓存',
  approved_count INT NOT NULL DEFAULT 0 COMMENT '已通过节数',
  submitted_count INT NOT NULL DEFAULT 0 COMMENT '待审定节数',
  returned_count INT NOT NULL DEFAULT 0 COMMENT '已打回节数',
  draft_count INT NOT NULL DEFAULT 0 COMMENT '草稿节数',
  assigned_by BIGINT NOT NULL COMMENT '最近一次分配操作人',
  assigned_at DATETIME NOT NULL COMMENT '最近一次分配时间',
  assignment_version INT NOT NULL DEFAULT 1 COMMENT '仅改派/取消递增，进度刷新不递增',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_chapter_assignment (tenant_id, chapter_id, deleted),
  KEY idx_tb_assignment_writer (tenant_id, writer_id, active, task_status),
  KEY idx_tb_assignment_reviewer (tenant_id, reviewer_id, active, submitted_count),
  KEY idx_tb_assignment_project (tenant_id, project_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材章分配（每章一条当前记录）';

-- ============================================================
-- 2) textbook_section_content 节当前正文
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_section_content (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '教材工程 ID（冗余便于授权与级联）',
  chapter_id BIGINT NOT NULL COMMENT '章 ID（冗余）',
  section_id BIGINT NOT NULL COMMENT '节节点 ID（textbook_outline.node_level=3）',
  content_md LONGTEXT NOT NULL COMMENT '当前 Markdown；初建可为空串',
  content_status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/submitted/approved/returned',
  word_count INT NOT NULL DEFAULT 0 COMMENT '后端按方案 §8.3 统一计算的纯文本字数',
  check_report_json TEXT NULL COMMENT '最近一次确定性检查报告 JSON',
  version INT NOT NULL DEFAULT 1 COMMENT '每次正文/状态写操作递增（baseVersion 乐观锁）',
  submitted_by BIGINT NULL COMMENT '当前这一轮提交人（自审校验用）',
  submitted_at DATETIME NULL COMMENT '当前这一轮提交时间',
  reviewed_by BIGINT NULL COMMENT '最近审定人；重新提交时清空',
  reviewed_at DATETIME NULL COMMENT '最近审定时间；重新提交时清空',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '保留首次创建人',
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_section_content (tenant_id, section_id, deleted),
  KEY idx_tb_content_chapter (tenant_id, project_id, chapter_id, content_status),
  KEY idx_tb_content_submitter (tenant_id, submitted_by, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材节当前正文（每节一条）';

-- ============================================================
-- 3) textbook_section_content_version 不可变快照
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_section_content_version (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  content_id BIGINT NOT NULL COMMENT 'textbook_section_content.id',
  section_id BIGINT NOT NULL COMMENT '节 ID',
  snapshot_no INT NOT NULL COMMENT '每 content 从 1 递增，所有快照共用序号',
  submit_revision INT NULL COMMENT '仅 submit 快照有值：第 1/2/3… 次提交',
  snapshot_type VARCHAR(16) NOT NULL COMMENT 'submit / before_restore',
  content_md LONGTEXT NOT NULL COMMENT '快照正文',
  word_count INT NOT NULL COMMENT '快照字数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '快照产生时的操作人',
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_content_version (tenant_id, content_id, snapshot_no, deleted),
  KEY idx_tb_version_submit (tenant_id, content_id, submit_revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材节正文快照（不可变）';

-- ============================================================
-- 4) textbook_section_review_record 提交与审定时间线
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_section_review_record (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  content_id BIGINT NOT NULL COMMENT 'textbook_section_content.id',
  section_id BIGINT NOT NULL COMMENT '节 ID',
  submit_revision INT NOT NULL COMMENT '对应第几次提交',
  action VARCHAR(16) NOT NULL COMMENT 'submit/approve/return/reopen',
  from_status VARCHAR(16) NOT NULL COMMENT '状态变化前',
  to_status VARCHAR(16) NOT NULL COMMENT '状态变化后',
  comment VARCHAR(1000) NULL COMMENT 'return/reopen 必填，approve 可填，submit 可空',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT '操作人',
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tb_review_section (tenant_id, section_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材节审定时间线';

-- ============================================================
-- 5) textbook_section_asset 正文图片归属
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_section_asset (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）；Markdown 中使用 textbook-asset://{id}',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '教材工程 ID（归属校验）',
  section_id BIGINT NOT NULL COMMENT '节 ID（归属校验）',
  file_id BIGINT NOT NULL COMMENT 'file 模块受保护（protected/**）文件 ID',
  asset_type VARCHAR(16) NOT NULL DEFAULT 'inline_image' COMMENT '本期仅 inline_image',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '上传顺序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_asset_file (tenant_id, file_id, deleted),
  KEY idx_tb_asset_section (tenant_id, section_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材节正文图片归属';
