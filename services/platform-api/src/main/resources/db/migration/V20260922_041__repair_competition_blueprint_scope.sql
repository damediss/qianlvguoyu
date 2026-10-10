-- 修复赛事“理论+实操综合测试1”挤压修模工中级的重复理论细目表。
-- v1 保留 1200 道理论题绑定；v2 没有题目、批次、组卷计划或试卷快照引用。
-- 仅逻辑删除错误版本，避免破坏历史数据；删除后 v1 成为唯一有效理论细目表。

-- 先让错误版本退出有效范围，避免随后启用 v1 触发唯一约束。
UPDATE competition_question_blueprint
   SET status = 'archived',
       updated_at = NOW()
 WHERE id = 2101963167190065154
   AND tenant_id = 10159
   AND event_id = 2101948885580668929
   AND event_work_type_id = 2101948916438163457
   AND event_work_type_level_id = 2101948941641736194
   AND content_type = 'theory'
   AND deleted = 0;

-- 若存在草稿组卷计划引用错误版本，切换到保留的 v1。
UPDATE competition_make_exam_plan
   SET blueprint_id = 2101950458650189826,
       updated_at = NOW()
 WHERE tenant_id = 10159
   AND event_id = 2101948885580668929
   AND event_work_type_id = 2101948916438163457
   AND event_work_type_level_id = 2101948941641736194
   AND content_type = 'theory'
   AND blueprint_id = 2101963167190065154
   AND status = 'draft'
   AND deleted = 0;

-- v1 是本范围唯一有效理论细目表，题目绑定保持不变。
UPDATE competition_question_blueprint
   SET status = 'active',
       updated_at = NOW()
 WHERE id = 2101950458650189826
   AND tenant_id = 10159
   AND event_id = 2101948885580668929
   AND event_work_type_id = 2101948916438163457
   AND event_work_type_level_id = 2101948941641736194
   AND content_type = 'theory'
   AND deleted = 0;

-- 仅在没有任何历史引用时逻辑删除 v2 及其节点/绑定；当前目标数据满足该条件。
UPDATE competition_question_blueprint
   SET deleted = 1,
       status = 'archived',
       updated_at = NOW()
 WHERE id = 2101963167190065154
   AND tenant_id = 10159
   AND deleted = 0
   AND NOT EXISTS (
     SELECT 1 FROM competition_make_exam_plan p
      WHERE p.tenant_id = 10159
        AND p.blueprint_id = 2101963167190065154
        AND p.deleted = 0
   )
   AND NOT EXISTS (
     SELECT 1 FROM competition_make_exam_paper_snapshot s
      WHERE s.tenant_id = 10159
        AND s.blueprint_id = 2101963167190065154
        AND s.deleted = 0
   )
   AND NOT EXISTS (
     SELECT 1 FROM competition_make_exam_paper_question q
      WHERE q.tenant_id = 10159
        AND q.blueprint_id = 2101963167190065154
        AND q.deleted = 0
   )
   AND NOT EXISTS (
     SELECT 1 FROM competition_question_import_batch b
      WHERE b.tenant_id = 10159
        AND b.blueprint_id = 2101963167190065154
        AND b.deleted = 0
   )
   AND NOT EXISTS (
     SELECT 1 FROM competition_question_blueprint_binding b
      WHERE b.tenant_id = 10159
        AND b.blueprint_id = 2101963167190065154
        AND b.deleted = 0
   );

UPDATE competition_question_blueprint_node n
   SET n.deleted = 1,
       n.updated_at = NOW()
 WHERE n.tenant_id = 10159
   AND n.blueprint_id = 2101963167190065154
   AND n.deleted = 0
   AND NOT EXISTS (
     SELECT 1 FROM competition_question_blueprint b
      WHERE b.tenant_id = 10159
        AND b.id = 2101963167190065154
        AND b.deleted = 0
   );

UPDATE competition_question_blueprint_binding b
   SET b.deleted = 1,
       b.updated_at = NOW()
 WHERE b.tenant_id = 10159
   AND b.blueprint_id = 2101963167190065154
   AND b.deleted = 0
   AND NOT EXISTS (
     SELECT 1 FROM competition_question_blueprint bp
      WHERE bp.tenant_id = 10159
        AND bp.id = 2101963167190065154
        AND bp.deleted = 0
   );
