-- 细目表管理：staging 两表 + 权限三步链（菜单已人工建好，id=2087737130031960065，code=xi_mu_biao_guan_li）
-- 方案：docs/04_requirements/职业技能标准细目表生成功能实现方案.md v1.2 §7/§10.1
-- 版本号依据：DB flyway_schema_history MAX(version)=20260813.001（2026-08-13 查询，坑#7）

-- ============================================================
-- 1) staging：解析任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS exam_standard_task (
  id               BIGINT       NOT NULL COMMENT '雪花ID',
  tenant_id        BIGINT       NOT NULL COMMENT '租户ID',
  work_type_title  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '职业名称',
  series_no        VARCHAR(64)  NULL COMMENT '职业编码，如 6-16-01-12',
  version          VARCHAR(32)  NULL COMMENT '版本年，如 2019',
  file_id          BIGINT       NULL COMMENT '上传 docx 文件ID（file 模块）',
  file_hash        VARCHAR(64)  NULL COMMENT '文件 SHA-256，重复上传检测',
  status           VARCHAR(20)  NOT NULL DEFAULT 'uploaded' COMMENT 'uploaded/parsed/llm_running/reviewing/confirmed/exported',
  directions_json  TEXT         NULL COMMENT '方向列表 JSON，如 ["电焊工","气焊工"]',
  levels_json      TEXT         NULL COMMENT '等级列表 JSON，如 [{"levelCode":0,"levelName":"五级/初级工"}]',
  weights_json     MEDIUMTEXT   NULL COMMENT '双权重表 JSON：{"theory":[{category,item,values:{"0|电焊工":25}}],"skill":[...]}',
  parse_warnings   TEXT         NULL COMMENT '解析警告（每行一条）',
  llm_model_id     VARCHAR(64)  NULL COMMENT 'LLM 概括所用模型',
  llm_finished_at  DATETIME     NULL COMMENT 'LLM 概括完成时间',
  deleted          TINYINT      NOT NULL DEFAULT 0,
  created_by       BIGINT       NULL,
  updated_by       BIGINT       NULL,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_standard_task_tenant (tenant_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='职业技能标准解析任务（staging）';

-- ============================================================
-- 2) staging：树节点草稿表
-- ============================================================
CREATE TABLE IF NOT EXISTS exam_standard_node (
  id                BIGINT       NOT NULL COMMENT '雪花ID',
  tenant_id         BIGINT       NOT NULL COMMENT '租户ID',
  task_id           BIGINT       NOT NULL COMMENT '所属任务',
  parent_id         BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点（本表 id），0=L0 之上',
  node_key          VARCHAR(64)  NOT NULL COMMENT '树内编码 A / A-A / A-A-A / A-A-A-001',
  node_level        INT          NOT NULL DEFAULT 0 COMMENT '层级 0~3',
  sort_number       INT          NOT NULL DEFAULT 100 COMMENT '同级排序',
  dimension         VARCHAR(10)  NOT NULL DEFAULT 'theory' COMMENT 'theory/skill，骨架节点恒为 theory',
  title_raw         TEXT         NULL COMMENT 'docx 抽取原文',
  title             VARCHAR(512) NOT NULL DEFAULT '' COMMENT '展示标题（含编码后缀，人工可改）',
  important         VARCHAR(4)   NULL COMMENT '重要程度 X/Y/Z（仅 L3）',
  weight            DECIMAL(6,4) NULL COMMENT '权重（如 0.2500），L0/L1 解析期落值，L2 可人工微调',
  related_knowledge TEXT         NULL COMMENT 'L2 相关知识原文（理论细目表"相关知识"列）',
  skill_requirement TEXT         NULL COMMENT 'L2 技能要求原文（技能细目表"技能要求"列）',
  source            VARCHAR(10)  NOT NULL DEFAULT 'extract' COMMENT 'extract/llm/manual',
  review_status     VARCHAR(10)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/modified',
  direction         VARCHAR(64)  NOT NULL DEFAULT '默认' COMMENT '所属方向（无方向工种为"默认"）',
  level_code        INT          NOT NULL DEFAULT 0 COMMENT '所属等级 0=五级/初级 … 4=一级/高级技师',
  need_llm          TINYINT      NOT NULL DEFAULT 0 COMMENT '1=L2 无列举项，需 LLM 概括补叶',
  deleted           TINYINT      NOT NULL DEFAULT 0,
  created_by        BIGINT       NULL,
  updated_by        BIGINT       NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_standard_node_key (task_id, level_code, direction(32), node_key),
  KEY idx_standard_node_task (task_id, deleted),
  KEY idx_standard_node_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='职业技能标准树节点草稿（staging）';

-- ============================================================
-- 3) 权限三步链（坑#8）：sys_permission → sys_role_menu → sys_role_permission
--    菜单已存在（deleted=0 已核实），此处只补权限码与授权
-- ============================================================

-- 3.1) sys_permission 3 条
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026081300201, 1, m.id, 'ximubiaoguanli:view', '查看细目表管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.id = 2087737130031960065 AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'ximubiaoguanli:view' AND p.deleted = 0
  );

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026081300202, 1, m.id, 'ximubiaoguanli:generate', '细目表上传解析与编辑', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.id = 2087737130031960065 AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'ximubiaoguanli:generate' AND p.deleted = 0
  );

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026081300203, 1, m.id, 'ximubiaoguanli:export', '导出细目表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.id = 2087737130031960065 AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'ximubiaoguanli:export' AND p.deleted = 0
  );

-- 3.2) sys_role_menu：平台管理员（role_id=2000）挂接菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026081300204, 1, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1 AND m.id = 2087737130031960065 AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 3.3) sys_role_permission：平台管理员授予全部 3 个权限码
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026081300205 + ROW_NUMBER() OVER (ORDER BY p.code),
  1,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.code IN ('ximubiaoguanli:view', 'ximubiaoguanli:generate', 'ximubiaoguanli:export')
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );
