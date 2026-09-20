-- AI 课件生成(courseware):建表 6 张 + 菜单权限三步链种子 + GLM 模型配置种子
-- 方案:docs/04_requirements/AI课件生成-标准docx转PPTX-方案.md v1.2(T1)
-- 菜单位置:智能课程材料管理(2087736644369305601)下,sort_order=4(两库现有子菜单最大 3)
-- 租户:种子一律 10159(千铝工投,运行租户;勿照抄旧文件的 tenant_id=1)
-- 版本号依据(2026-09-20 双库查询):gjjddb2026(测试) MAX=20260918.005;gjjddbpro(生产) MAX=20260916.001,
-- 本迁移取 20260920.101,在两库最大值之上并留足同日期间隔(踩坑 #7)。

-- ============================================================
-- 1) cw_project 课件项目
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_project (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  title VARCHAR(200) NOT NULL COMMENT '课件标题',
  source_file_id BIGINT NOT NULL COMMENT '原始 docx 文件 ID(sys_file.id)',
  audience VARCHAR(100) NULL DEFAULT '学员自学' COMMENT '受众',
  content_density VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT '文字量档位 concise/medium/detailed',
  theme_id VARCHAR(32) NOT NULL DEFAULT 'enterprise' COMMENT '主题 ID(一期固定 enterprise)',
  status VARCHAR(24) NOT NULL DEFAULT 'draft' COMMENT 'draft草稿 parsing解析中 digesting整理素材中 outline_generating大纲生成中 outline_ready待确认 generating生成中 ready完成 partial部分失败 failed失败 cancelled已取消',
  total_pages INT NOT NULL DEFAULT 0 COMMENT '总页数',
  ready_pages INT NOT NULL DEFAULT 0 COMMENT '已完成页数',
  failed_pages INT NOT NULL DEFAULT 0 COMMENT '失败页数',
  error VARCHAR(500) NULL COMMENT '项目级错误(白话)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_proj_tenant (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件项目';

-- ============================================================
-- 2) cw_source_section 来源小节(docx 解析产物)
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_source_section (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '课件项目 ID',
  ref VARCHAR(32) NOT NULL COMMENT '稳定引用号 S{来源}:{小节},一期恒 S1:N',
  heading VARCHAR(200) NULL COMMENT '小节标题(无标题小节为 NULL)',
  level INT NOT NULL DEFAULT 0 COMMENT '标题级别 0~6',
  text MEDIUMTEXT NULL COMMENT '小节正文(表格已管道化)',
  locator VARCHAR(64) NULL COMMENT '定位描述(第 N 段)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_section_project (project_id),
  KEY idx_cw_section_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件来源小节';

-- ============================================================
-- 3) cw_digest 消化素材(每项目一份;失败无记录走降级)
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_digest (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '课件项目 ID',
  topics JSON NULL COMMENT '消化产物 [{name,points[],confusions[],key_data[],source_refs[]}]',
  status VARCHAR(16) NOT NULL DEFAULT 'ready' COMMENT 'ready/failed',
  error VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_cw_digest_project (project_id),
  KEY idx_cw_digest_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件消化素材';

-- ============================================================
-- 4) cw_outline_page 大纲页(章→页两级)
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_outline_page (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '课件项目 ID',
  chapter_no INT NOT NULL DEFAULT 0 COMMENT '所属章序号(0=章外页:cover/toc/review)',
  chapter_title VARCHAR(120) NULL COMMENT '所属章标题(冗余,便于分组展示)',
  position INT NOT NULL COMMENT '页序(全文档连续)',
  title VARCHAR(120) NOT NULL COMMENT '页标题(内容页应为结论句)',
  objective VARCHAR(300) NULL COMMENT '本页目标',
  key_points JSON NULL COMMENT '要点数组 3~5 条',
  page_role VARCHAR(16) NOT NULL DEFAULT 'content' COMMENT 'cover/toc/section/content/summary/review',
  layout_id VARCHAR(32) NULL COMMENT '槽位模板 ID(编排层分配)',
  source_refs JSON NULL COMMENT '本页引用的 ref 数组',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_outline_project (project_id, position),
  KEY idx_cw_outline_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件大纲页';

-- ============================================================
-- 5) cw_slide 生成页
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_slide (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '课件项目 ID',
  outline_page_id BIGINT NULL COMMENT '大纲页 ID',
  position INT NOT NULL COMMENT '页序',
  layout_id VARCHAR(32) NULL COMMENT '实际使用的模板',
  blocks JSON NULL COMMENT '内容块数组(单一事实源)',
  speaker_notes VARCHAR(1000) NULL COMMENT '讲稿提示(写入 PPTX 备注页)',
  issues JSON NULL COMMENT '残留质量告警',
  status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/generating/ready/failed/cancelled',
  error VARCHAR(500) NULL,
  revision INT NOT NULL DEFAULT 0 COMMENT '乐观锁(重试/覆盖写保护)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_slide_project_status (project_id, status),
  KEY idx_cw_slide_project_pos (project_id, position),
  KEY idx_cw_slide_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件生成页';

-- ============================================================
-- 6) cw_llm_log LLM 调用审计(LLM 规范 #10)
-- ============================================================
CREATE TABLE IF NOT EXISTS cw_llm_log (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NULL COMMENT '课件项目 ID',
  slide_id BIGINT NULL COMMENT '生成页 ID(单页阶段)',
  stage VARCHAR(24) NOT NULL COMMENT 'digest/outline/slide',
  model_code VARCHAR(64) NULL COMMENT '模型配置编码',
  model_id VARCHAR(64) NULL COMMENT '实际模型 ID',
  prompt_digest VARCHAR(200) NULL COMMENT 'prompt 摘要(sha256+长度)',
  prompt_tokens INT NULL,
  completion_tokens INT NULL,
  total_tokens INT NULL,
  cached_tokens INT NULL,
  duration_ms BIGINT NULL,
  success TINYINT NOT NULL DEFAULT 1,
  error VARCHAR(500) NULL,
  trace_id VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_llm_log_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件 LLM 调用审计';

-- ============================================================
-- 7) sys_menu:智能课程材料管理目录下新增"AI课件生成管理"
-- ============================================================
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026092000101, 10159, 2087736644369305601, 'menu',
  'ai_courseware_gen', 'AI课件生成管理',
  '/ai-ke-jian-sheng-cheng', 'zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage',
  'courseware:project:list', 'FilePptOutlined', 4, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
);

-- 8) sys_permission 6 条
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000201, 10159, m.id, 'courseware:project:list', '查看课件项目', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:list' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000202, 10159, m.id, 'courseware:project:create', '创建课件项目', 'create', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:create' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000203, 10159, m.id, 'courseware:project:confirm', '确认课件大纲', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:confirm' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000204, 10159, m.id, 'courseware:project:generate', '触发生成/取消课件', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:generate' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000205, 10159, m.id, 'courseware:project:retry', '重试课件页面', 'retry', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:retry' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000206, 10159, m.id, 'courseware:project:export', '导出课件 PPTX', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'courseware:project:export' AND p.deleted = 0);

-- 9) sys_role_menu:平台管理员(role_id=2000)挂接菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026092000301, 10159, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'ai_courseware_gen' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 10159 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 10) sys_role_permission:平台管理员授予全部权限码
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  2026092000401 + ROW_NUMBER() OVER (ORDER BY p.code),
  10159,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code IN (
    'courseware:project:list', 'courseware:project:create', 'courseware:project:confirm',
    'courseware:project:generate', 'courseware:project:retry', 'courseware:project:export'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- ============================================================
-- 11) 模型配置种子:GLM-5.3-Flash(thinking 关闭,方案 §12)
-- api_key 为占位符,部署方手工回填真实 key;严禁真实 key 出现在仓库任何文件
-- ============================================================
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, thinking, reasoning_effort, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  2026092000102, 10159,
  'courseware-gen-glm',
  'GLM-5.3-Flash(课件生成)',
  'https://open.bigmodel.cn/api/paas/v4',
  'PENDING_USER_INPUT',
  'openai_compatible',
  'GLM-5.3-Flash',
  0.70, 8192,
  -- GLM-5.3-Flash 始终思考、不支持 thinking=disabled(错误码 1210),按可用档位 low;
  -- 思维链计入输出 token,max_tokens 与理论生成同口径 8192 留足预算
  'enabled', 'low', NULL,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  base_url = VALUES(base_url),
  model_id = VALUES(model_id),
  thinking = VALUES(thinking),
  reasoning_effort = VALUES(reasoning_effort),
  updated_at = NOW();
