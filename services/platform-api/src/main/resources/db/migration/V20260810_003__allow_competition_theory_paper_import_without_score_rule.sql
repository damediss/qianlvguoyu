-- 课程组卷计划已经携带试卷时长、分值和题型要求。
-- 理论试卷快照按赛事、赛段、工种和课程试卷固化，不再强制绑定竞赛评分规则或理论要求。
ALTER TABLE competition_theory_paper_snapshot
  MODIFY COLUMN score_rule_id BIGINT NULL,
  MODIFY COLUMN requirement_id BIGINT NULL;
