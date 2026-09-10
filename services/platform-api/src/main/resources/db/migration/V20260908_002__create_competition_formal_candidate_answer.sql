-- 正式比赛选手当前答案。一个 CandidateExam + 一道正式题目快照最多一条当前答案。
-- 本阶段不写评分字段，不落答案修改历史。

CREATE TABLE competition_formal_candidate_answer (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  question_snapshot_id BIGINT NOT NULL,
  answer_json LONGTEXT NULL,
  is_answered TINYINT NOT NULL DEFAULT 0,
  is_marked TINYINT NOT NULL DEFAULT 0,
  answer_version INT NOT NULL,
  first_answer_time DATETIME NULL,
  last_answer_time DATETIME NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_candidate_answer_answered CHECK (is_answered IN (0, 1)),
  CONSTRAINT chk_comp_formal_candidate_answer_marked CHECK (is_marked IN (0, 1)),
  CONSTRAINT chk_comp_formal_candidate_answer_version CHECK (answer_version > 0),
  UNIQUE KEY uk_comp_formal_candidate_answer_question (
    tenant_id, candidate_exam_id, question_snapshot_id, active_record_key
  ),
  KEY idx_comp_formal_candidate_answer_session (
    tenant_id, exam_session_id, candidate_exam_id, deleted
  ),
  KEY idx_comp_formal_candidate_answer_candidate (
    tenant_id, candidate_exam_id, is_answered, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛选手当前答案';
