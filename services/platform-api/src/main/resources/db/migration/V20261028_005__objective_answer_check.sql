-- 客观题答案 AI 检查（一期）三表 + 菜单/权限/角色授权（方案 §12）
-- 取号：2026-10-28 实施当天实测测试库 gjjddb2026 MAX=20261028.003、生产库 gjjddbpro MAX=20261027.006，
--       共用目录已存在 V20261028_004，故本文件取 20261028.005。
-- 全部幂等；tenant_id 固定 10159；一期只给平台管理员 role_id=2000 授权。
--
-- 注意：本文件不含任何 API Key / 模型配置；AI 凭据只走环境变量。

-- ────────────────────────────── 1. 批次表 ──────────────────────────────
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_batch (
  id BIGINT NOT NULL COMMENT '批次ID，雪花；同时作为对外批次号',
  tenant_id BIGINT NOT NULL,
  active_tenant_id BIGINT NULL COMMENT '活动批次唯一槽；paused/done及无processing的failed置NULL',
  run_id BIGINT NULL COMMENT '编排代次；旧run禁止派发和收尾',
  owner_id VARCHAR(128) NULL COMMENT '编排所有者hostname:pid:UUID',
  lease_until DATETIME NULL COMMENT '编排租约，数据库时间；过期才可接管',
  selection_mode VARCHAR(8) NOT NULL COMMENT 'ids/filter',
  filter_json LONGTEXT NOT NULL COMMENT '筛选快照，不含API Key和逐题IDs',
  status VARCHAR(16) NOT NULL,
  total INT NOT NULL DEFAULT 0,
  done INT NOT NULL DEFAULT 0,
  failed INT NOT NULL DEFAULT 0,
  invalid INT NOT NULL DEFAULT 0,
  consistent_high INT NOT NULL DEFAULT 0,
  mismatch_high INT NOT NULL DEFAULT 0,
  low_confidence INT NOT NULL DEFAULT 0,
  content_suspect INT NOT NULL DEFAULT 0 COMMENT '内容质量suspect行数，跨AI结果分类',
  content_invalid INT NOT NULL DEFAULT 0 COMMENT '内容质量invalid行数，是result invalid子集',
  expected_calls INT NOT NULL DEFAULT 0,
  call_count INT NOT NULL DEFAULT 0,
  input_tokens_exact BIGINT NOT NULL DEFAULT 0,
  input_tokens_estimated BIGINT NOT NULL DEFAULT 0,
  output_tokens BIGINT NOT NULL DEFAULT 0,
  usage_estimated_calls INT NOT NULL DEFAULT 0,
  model_duration_ms BIGINT NOT NULL DEFAULT 0,
  model_id VARCHAR(64) NOT NULL,
  prompt_version VARCHAR(32) NOT NULL,
  confidence_threshold DECIMAL(4,3) NOT NULL,
  margin_threshold DECIMAL(4,3) NOT NULL,
  fail_reason VARCHAR(500) NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_objective_answer_check_active (active_tenant_id),
  KEY idx_oacb_tenant_status (tenant_id, status, id),
  KEY idx_oacb_lease (status, lease_until),
  KEY idx_oacb_created (tenant_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查批次';

-- ────────────────────────────── 2. 明细表 ──────────────────────────────
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  detail_id INT NOT NULL COMMENT 'exam_exam_detail.id',
  question_type VARCHAR(16) NOT NULL,
  source INT NULL,
  work_type_id INT NULL,
  work_type_title VARCHAR(512) NULL,
  level_code INT NULL COMMENT '对外等级1~5；由5-exam_course.level转换',
  course_id INT NULL,
  course_title VARCHAR(512) NULL,
  knowledge_id INT NULL,
  knowledge_path VARCHAR(1024) NULL,
  content_snapshot TEXT NULL,
  stem_text TEXT NOT NULL,
  answer_snapshot LONGTEXT NULL COMMENT '原始answer；SQL NULL也保留为NULL并由预检标记异常',
  resolve_snapshot TEXT NULL,
  option_snapshot_json LONGTEXT NOT NULL COMMENT '归一选项及原正确标记',
  source_hash CHAR(64) NOT NULL COMMENT 'type+content+answer 的SHA-256',
  content_quality VARCHAR(16) NOT NULL DEFAULT 'clean' COMMENT 'clean/suspect/invalid',
  content_warnings_json LONGTEXT NULL COMMENT '结构化内容警告，原值不被suggestedText覆盖',
  process_status VARCHAR(16) NOT NULL,
  result_code VARCHAR(24) NULL,
  consistent TINYINT NULL,
  original_answer_keys VARCHAR(128) NULL,
  ai_answer_keys VARCHAR(128) NULL,
  reference_confidence DECIMAL(6,5) NULL,
  ai_confidence DECIMAL(6,5) NULL,
  min_margin DECIMAL(6,5) NULL,
  option_results_json LONGTEXT NULL COMMENT '逐选项概率与判断；仅成功后完整写入',
  decision_warning VARCHAR(64) NULL,
  invalid_reason VARCHAR(64) NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(500) NULL,
  retryable TINYINT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0 COMMENT '认领代次；结果写入fencing，不在重试时清零',
  version INT NOT NULL DEFAULT 0 COMMENT '全行变更版本；人工复核乐观锁',
  review_status VARCHAR(24) NOT NULL DEFAULT 'unreviewed',
  review_note VARCHAR(500) NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  checked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_oaci_batch_detail (batch_id, detail_id),
  KEY idx_oaci_batch_process (batch_id, process_status, id),
  KEY idx_oaci_batch_result (batch_id, result_code, id),
  KEY idx_oaci_batch_content (batch_id, content_quality, id),
  KEY idx_oaci_batch_review (batch_id, review_status, id),
  KEY idx_oaci_detail (tenant_id, detail_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查明细快照';

-- ────────────────────────────── 3. 调用审计表 ──────────────────────────────
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_call_log (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  detail_id INT NOT NULL,
  run_id BIGINT NOT NULL COMMENT '发起该调用的编排代次',
  claim_attempt INT NOT NULL COMMENT '发起该调用的明细认领代次',
  call_id VARCHAR(36) NOT NULL COMMENT '每个逻辑块的本地UUID，重试同一轮可关联',
  upstream_request_id VARCHAR(128) NULL,
  tool_name VARCHAR(64) NOT NULL DEFAULT 'objective_answer_check' COMMENT '受控AI工具名',
  prompt_version VARCHAR(32) NOT NULL,
  chunk_index INT NOT NULL DEFAULT 1,
  chunk_count INT NOT NULL DEFAULT 1,
  question_type VARCHAR(16) NOT NULL,
  requested_model VARCHAR(64) NOT NULL,
  response_model VARCHAR(64) NULL,
  confidence_definition VARCHAR(128) NULL,
  prompt_sha256 CHAR(64) NOT NULL,
  prompt_preview VARCHAR(200) NULL COMMENT '题干末尾摘要，不含API Key和原答案标记',
  input_tokens BIGINT NULL,
  output_tokens BIGINT NULL,
  usage_estimated TINYINT NOT NULL DEFAULT 0,
  duration_ms INT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'success/failed',
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_oacll_call (call_id),
  KEY idx_oacll_item (item_id, id),
  KEY idx_oacll_batch (batch_id, status, id),
  KEY idx_oacll_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查模型调用审计';

-- ────────────────────────────── 4. 菜单 ──────────────────────────────
SET @tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

-- 父菜单：优先“资源管理”(zi_yuan_guan_li)，回退 question_resource；不得 parent_id=0 兜底
SET @parent_menu_id := COALESCE(
  (SELECT id FROM sys_menu
   WHERE tenant_id = @tenant_id AND code = 'zi_yuan_guan_li' AND deleted = 0
   ORDER BY id LIMIT 1),
  (SELECT id FROM sys_menu
   WHERE tenant_id = @tenant_id AND code = 'question_resource' AND deleted = 0
   ORDER BY id LIMIT 1)
);

SET @oac_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'objective_answer_check' AND deleted = 0
  ORDER BY id LIMIT 1
), 202610280101);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, expanded, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @oac_menu_id, @tenant_id, @parent_menu_id, 'page', 'objective_answer_check', '客观题答案AI检查',
  '/question-bank/resources/objective-answer-check', 'question_bank/ObjectiveAnswerCheckPage',
  'exam:objective-answer-check:view', 'SafetyCertificateOutlined', 6, 1, 1, 'enabled',
  NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM DUAL
WHERE @parent_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), name = VALUES(name),
  path = VALUES(path), component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), sort_order = VALUES(sort_order), visible = 1, expanded = 1,
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

SET @oac_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @tenant_id AND code = 'objective_answer_check' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- ────────────────────────────── 5. 权限码 ──────────────────────────────
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202610280201, @tenant_id, @oac_menu_id, 'exam:objective-answer-check:view',
       '客观题答案AI检查-查看', 'view', 'enabled', NOW(), NOW(), @seed_operator_id,
       @seed_operator_id, 0
FROM DUAL WHERE @oac_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202610280202, @tenant_id, @oac_menu_id, 'exam:objective-answer-check:submit',
       '客观题答案AI检查-发起与暂停', 'submit', 'enabled', NOW(), NOW(), @seed_operator_id,
       @seed_operator_id, 0
FROM DUAL WHERE @oac_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202610280203, @tenant_id, @oac_menu_id, 'exam:objective-answer-check:review',
       '客观题答案AI检查-人工复核', 'review', 'enabled', NOW(), NOW(), @seed_operator_id,
       @seed_operator_id, 0
FROM DUAL WHERE @oac_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202610280204, @tenant_id, @oac_menu_id, 'exam:objective-answer-check:export',
       '客观题答案AI检查-导出', 'export', 'enabled', NOW(), NOW(), @seed_operator_id,
       @seed_operator_id, 0
FROM DUAL WHERE @oac_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = VALUES(updated_by);

-- ────────────────────────────── 6. 角色授权（仅平台管理员，显式连接 10159 角色） ──────────────────────────────
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, created_at, updated_at, created_by,
  updated_by, deleted)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_role_menu), @tenant_id, 2000, @oac_menu_id,
       NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.id = 2000 AND r.tenant_id = @tenant_id AND r.deleted = 0
  AND @oac_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = @tenant_id AND rm.role_id = 2000 AND rm.menu_id = @oac_menu_id
      AND rm.deleted = 0
  );

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, created_at, updated_at,
  created_by, updated_by, deleted)
SELECT (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission) + ROW_NUMBER() OVER (ORDER BY p.id),
       @tenant_id, 2000, p.id,
       NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_permission p
JOIN sys_role r ON r.id = 2000 AND r.tenant_id = @tenant_id AND r.deleted = 0
WHERE p.tenant_id = @tenant_id AND p.deleted = 0
  AND p.code IN ('exam:objective-answer-check:view', 'exam:objective-answer-check:submit',
                 'exam:objective-answer-check:review', 'exam:objective-answer-check:export')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = @tenant_id AND rp.role_id = 2000 AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
