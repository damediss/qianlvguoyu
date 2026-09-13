-- 知识归属修正：工作表 + 菜单/权限三步链（坑#8）+ 旗舰模型配置种子
-- 方案：docs/04_requirements/exam_exam_detail知识归属修正方案_gjjddbpro.md v1.2（§7）
-- 挂载点：课程管理(4700) → 资源管理(新目录) → 知识归属修正(页面)
-- 租户：种子一律 10159（千铝工投运行租户，坑#8；勿照抄旧文件的 tenant_id=1）
-- 版本号依据（2026-09-12 双库实查，踩坑#7）：gjjddbpro MAX=20260911.014；gjjddb2026 MAX=20260911.011
-- 本迁移取 20260912.001（大于双库最大值）

-- 1) 去重清单表
CREATE TABLE IF NOT EXISTS exam_detail_dedup_list (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  detail_id INT NOT NULL,
  group_key VARCHAR(32) NOT NULL,
  is_keeper TINYINT NOT NULL DEFAULT 0,
  keeper_id INT NULL,
  create_wave VARCHAR(16) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_kfdl_detail (detail_id),
  KEY idx_kfdl_group (group_key),
  KEY idx_kfdl_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='exam_exam_detail 去重清单（工作底表+审计）';

-- 2) 知识修正清单表
CREATE TABLE IF NOT EXISTS exam_detail_knowledge_fix (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  detail_id INT NOT NULL,
  old_knowledge_id INT NULL,
  course_id INT NULL,
  new_knowledge_id INT NULL,
  confidence DECIMAL(4,3) NULL,
  basis VARCHAR(16) NULL,
  llm_reason VARCHAR(200) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  batch_no BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_kffd_detail (detail_id),
  KEY idx_kffd_status (status),
  KEY idx_kffd_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='exam_exam_detail 知识修正清单（工作底表+审计）';

-- 3) 分类批次表
CREATE TABLE IF NOT EXISTS exam_knowledge_fix_batch (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  batch_no BIGINT NULL,
  total INT NOT NULL DEFAULT 0,
  done INT NOT NULL DEFAULT 0,
  failed INT NOT NULL DEFAULT 0,
  apply_cnt INT NOT NULL DEFAULT 0,
  keep_cnt INT NOT NULL DEFAULT 0,
  flag_cnt INT NOT NULL DEFAULT 0,
  self_consistency_rate DECIMAL(5,4) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  fail_reason VARCHAR(300) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_kfkb_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识归属修正 LLM 分类批次';

-- 3.1) LLM 调用审计表（LLM 规范 #10：调用落库）
CREATE TABLE IF NOT EXISTS exam_knowledge_fix_llm_log (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  batch_no BIGINT NULL,
  detail_id INT NULL,
  stage VARCHAR(16) NULL,
  model_id VARCHAR(64) NULL,
  prompt_digest VARCHAR(200) NULL,
  prompt_tokens INT NULL,
  completion_tokens INT NULL,
  total_tokens INT NULL,
  cached_tokens INT NULL,
  duration_ms INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_kfll_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识归属修正 LLM 调用审计';

-- 4) 菜单：课程管理(4700) → 资源管理（目录）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202609120101, 10159, 4700, 'directory',
  'question_resource', '资源管理',
  '/question-bank/resources', 'question_bank/ResourceCenterPage',
  'question.resource:view', 'FolderOutlined', 3, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.tenant_id = 10159 AND m.code = 'question_resource' AND m.deleted = 0
);

-- 5) 菜单：资源管理 → 知识归属修正（页面）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component,
  permission_code, icon, sort_order, visible, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202609120102, 10159, 202609120101, 'menu',
  'knowledge_fix', '知识归属修正',
  '/question-bank/resources/knowledge-fix', 'question_bank/KnowledgeFixPage',
  'exam:knowledgefix:view', 'ToolOutlined', 1, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
);

-- 6) sys_permission 8 条（挂知识归属修正菜单）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120201, 10159, m.id, 'exam:knowledgefix:view', '查看知识归属修正', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:view' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120202, 10159, m.id, 'exam:knowledgefix:dedup:generate', '生成去重清单', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:dedup:generate' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120203, 10159, m.id, 'exam:knowledgefix:dedup:apply', '执行去重软删除', 'apply', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:dedup:apply' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120204, 10159, m.id, 'exam:knowledgefix:classify:submit', '提交知识分类批次', 'submit', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:classify:submit' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120205, 10159, m.id, 'exam:knowledgefix:classify:export', '导出修正清单', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:classify:export' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120206, 10159, m.id, 'exam:knowledgefix:fix:apply', '执行知识修正写回', 'apply', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:fix:apply' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120207, 10159, m.id, 'exam:knowledgefix:rollback', '回滚修正/去重', 'rollback', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:rollback' AND p.deleted = 0);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609120208, 10159, m.id, 'exam:knowledgefix:report:view', '查看质量报告', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code = 'knowledge_fix' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.tenant_id = 10159 AND p.code = 'exam:knowledgefix:report:view' AND p.deleted = 0);

-- 7) sys_role_menu：平台管理员（role_id=2000）挂接两个菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202609121101 + ROW_NUMBER() OVER (ORDER BY m.code), 10159, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 10159 AND m.code IN ('question_resource', 'knowledge_fix') AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 10159 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 8) sys_role_permission：平台管理员授予全部权限码
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202609121401 + ROW_NUMBER() OVER (ORDER BY p.code),
  10159,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code IN (
    'exam:knowledgefix:view', 'exam:knowledgefix:dedup:generate', 'exam:knowledgefix:dedup:apply',
    'exam:knowledgefix:classify:submit', 'exam:knowledgefix:classify:export',
    'exam:knowledgefix:fix:apply', 'exam:knowledgefix:rollback', 'exam:knowledgefix:report:view'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- 9) 旗舰复判模型配置（api_key 部署回填，与 theory-gen-glm 行同 key；严禁真实 key 入库）
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, thinking, reasoning_effort, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202609120002, 10159, 'knowledge-fix-pro', '知识修正旗舰复判',
  'https://open.bigmodel.cn/api/paas/v4', 'PENDING_USER_INPUT', 'openai_compatible', 'glm-5.3',
  0.70, 4096, 'enabled', 'low', NULL,
  'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM exam_faq_model_config c
  WHERE c.model_code = 'knowledge-fix-pro' AND c.deleted = 0
);
