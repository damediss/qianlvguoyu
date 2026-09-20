-- 版本号依据：已查询两个运行库的 flyway_schema_history。
-- gjjddb2026 MAX(version)=20260916.002；gjjddbpro MAX(version)=20260915.002。
-- 生产库会先正常执行待应用的 20260916 迁移，再执行本迁移。

-- 随计划保存种子，使预检约定的确定性选择可以在正式生成和后续重生成时复现。
ALTER TABLE competition_make_exam_plan
  ADD COLUMN random_seed BIGINT NULL COMMENT '实操自动组卷确定性随机种子' AFTER paper_count;

-- 理论仍由应用层限制为单套；数据库只约束套数为有效范围，实操蓝图计划可生成多套。
ALTER TABLE competition_make_exam_plan
  DROP CHECK chk_competition_make_plan_papers,
  ADD CONSTRAINT chk_competition_make_plan_papers CHECK (paper_count BETWEEN 1 AND 50);

-- 实操多套快照使用 P1、P2...；历史单套 P 和理论历史 A/B/P 均保持兼容。
ALTER TABLE competition_make_exam_paper_snapshot
  DROP CHECK chk_competition_make_snapshot_code,
  ADD CONSTRAINT chk_competition_make_snapshot_code CHECK (
    (content_type = 'theory' AND paper_code IN ('A', 'B', 'P'))
    OR (content_type = 'practical' AND paper_code REGEXP '^P([1-9][0-9]*)?$')
  );
