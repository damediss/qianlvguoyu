-- ============================================================================
-- AI 同步学单库化 · 一次性数据修复（方案 §4-H，gjjddbpro，部署窗口内执行）
--
-- 前置：服务端已上线（/appreviewsync/** 强制租户 10159 生效），此后不再产生 tenant_id=1 新行。
-- 执行顺序不可调换：先归一（步骤 1/2），再补写 exam_result（步骤 3）。
-- 全程非 Flyway（一次性运维）；每步先跑 SELECT 复核行数与方案 §6.1 实测一致，再执行写操作。
-- ============================================================================

-- ---------- 步骤 0：复核（预期 plan=2 / plan_day=2 / answer=58 / wrong_item=26 / 知识树=1） ----------
SELECT 'ai_review_plan' AS tbl, COUNT(*) AS cnt FROM ai_review_plan WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_plan_day', COUNT(*) FROM ai_review_plan_day WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_answer', COUNT(*) FROM ai_review_answer WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_wrong_item', COUNT(*) FROM ai_review_wrong_item WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'exam_knowledge_structure', COUNT(*) FROM exam_knowledge_structure WHERE tenant_id = 1 AND deleted = 0;

-- ---------- 步骤 1：ai_review_* 遗留 tenant_id=1 归一为 10159 ----------
-- 唯一键 uk_ai_review_plan_user_exam=(platform_user_id, exam_id) 不含 tenant_id，UPDATE 不撞键
UPDATE ai_review_plan SET tenant_id = 10159 WHERE tenant_id = 1 AND deleted = 0;
UPDATE ai_review_plan_day SET tenant_id = 10159 WHERE tenant_id = 1 AND deleted = 0;
UPDATE ai_review_answer SET tenant_id = 10159 WHERE tenant_id = 1 AND deleted = 0;
UPDATE ai_review_wrong_item SET tenant_id = 10159 WHERE tenant_id = 1 AND deleted = 0;

-- ---------- 步骤 2：知识树 id=13910（course 205「职业道德」L1）归一 ----------
-- 先人工确认该节点归属 10159；若认定部确认为误建垃圾行，改为 DELETE（逻辑删）而非 UPDATE
SELECT id, course_id, parent_id, title, level, tenant_id FROM exam_knowledge_structure WHERE id = 13910;
UPDATE exam_knowledge_structure SET tenant_id = 10159 WHERE id = 13910 AND tenant_id = 1;

-- ---------- 步骤 3：58 行 php_synced=0 作答补写 exam_result（对齐 PHP trainsave 口径） ----------
-- 复核待补写行数（预期 58：步骤 1 归一后 tenant_id=10159 且 php_synced=0）
SELECT COUNT(*) AS pending_cnt FROM ai_review_answer WHERE tenant_id = 10159 AND php_synced = 0 AND deleted = 0;

INSERT INTO exam_result
  (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id, platform_user_id,
   user_answer, tenant_id, create_time, deleted)
SELECT a.question_type,
       a.exam_plan_id,
       0,
       0,
       COALESCE(a.earned_score, IF(a.is_correct = 1, a.question_score, 0)),
       5,
       a.question_id,
       a.platform_user_id,
       a.user_answer,
       10159,
       COALESCE(a.created_at, NOW()),
       0
FROM ai_review_answer a
WHERE a.tenant_id = 10159 AND a.php_synced = 0 AND a.deleted = 0;

-- 补写完成后置位，防止任何残留补偿逻辑重写
UPDATE ai_review_answer SET php_synced = 1, php_sync_time = NOW()
WHERE tenant_id = 10159 AND php_synced = 0 AND deleted = 0;

-- ---------- 步骤 4：sys_user 脏 tenant_id 行人工确认（归一化遗留，顺带处理） ----------
-- 先 SELECT 确认内容，人工认定后再决定 UPDATE tenant_id=10159 或逻辑删除
SELECT id, username, nickname, tenant_id, deleted, created_at FROM sys_user WHERE tenant_id = 2062723418189152258;

-- ---------- 验收：修复后应全为 0 ----------
SELECT 'ai_review_plan' AS tbl, COUNT(*) AS cnt FROM ai_review_plan WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_plan_day', COUNT(*) FROM ai_review_plan_day WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_answer', COUNT(*) FROM ai_review_answer WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'ai_review_wrong_item', COUNT(*) FROM ai_review_wrong_item WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'exam_knowledge_structure', COUNT(*) FROM exam_knowledge_structure WHERE tenant_id = 1 AND deleted = 0
UNION ALL
SELECT 'answer_php_synced_0', COUNT(*) FROM ai_review_answer WHERE php_synced = 0 AND deleted = 0;

-- ============================================================================
-- 回滚防重写（方案 §9.2，仅在代码回滚旧 jar 时执行）：
--   回滚前记录部署时刻 ai_review_answer 的 MAX(id)，回滚后执行：
--   UPDATE ai_review_answer SET php_synced = 1
--   WHERE php_synced = 0 AND id > :maxIdBeforeDeploy;
-- ============================================================================
