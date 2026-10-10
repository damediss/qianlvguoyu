-- 每个竞赛组卷计划只负责生成一套试卷；A/B 或更多套试卷由多个计划分别生成，
-- 再由赛段编排选择并在排考/选手配置阶段分配。

-- 已存在的理论草稿可能包含历史 A/B 快照。保留 A 作为可继续编辑的草稿，
-- 将多余的 B 草稿标记为废弃，避免旧计划无法迁移到单套试卷模型。
UPDATE competition_make_exam_paper_snapshot s
INNER JOIN competition_make_exam_plan p ON p.id = s.plan_id
SET s.status = 'deprecated',
    s.updated_at = NOW()
WHERE p.status = 'draft'
  AND p.content_type = 'theory'
  AND s.paper_code = 'B'
  AND s.status = 'draft'
  AND s.deleted = 0;

ALTER TABLE competition_make_exam_plan
  DROP CHECK chk_competition_make_plan_papers,
  MODIFY COLUMN paper_count INT NOT NULL DEFAULT 1;

-- 先移除旧的 theory=2 约束后再修正存量数据，避免 UPDATE 被旧约束拦截。
UPDATE competition_make_exam_plan
SET paper_count = 1,
    updated_at = NOW()
WHERE paper_count <> 1;

ALTER TABLE competition_make_exam_plan
  ADD CONSTRAINT chk_competition_make_plan_papers CHECK (paper_count = 1);

-- A/B 是历史兼容值，P 是新建单套计划的统一卷别标识。
ALTER TABLE competition_make_exam_paper_snapshot
  DROP CHECK chk_competition_make_snapshot_code,
  ADD CONSTRAINT chk_competition_make_snapshot_code CHECK (
    (content_type = 'theory' AND paper_code IN ('A', 'B', 'P'))
    OR (content_type = 'practical' AND paper_code = 'P')
  );

-- 赛段场次卷别与执行快照一致，支持 P1、P2 等由赛段编排生成的变体编号。
ALTER TABLE competition_theory_exam_session
  DROP CHECK chk_comp_theory_session_code,
  ADD CONSTRAINT chk_comp_theory_session_code CHECK (
    session_code REGEXP '^[A-Za-z][A-Za-z0-9_-]{0,15}$'
  );
