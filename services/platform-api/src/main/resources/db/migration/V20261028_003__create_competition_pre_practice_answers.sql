-- Each dispatch recipient is an independent practice attempt, including repeated pushes of the same task.
CREATE TABLE IF NOT EXISTS competition_pre_practice_answer (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  delivery_id BIGINT NOT NULL COMMENT 'competition_pre_practice_dispatch_recipient.id',
  task_id BIGINT NOT NULL,
  task_question_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  answer_json LONGTEXT NOT NULL COMMENT 'Latest answer value encoded as JSON; empty answers are stored as JSON null',
  answered_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pre_practice_answer_delivery_question
    (tenant_id, delivery_id, task_question_id),
  KEY idx_pre_practice_answer_delivery (tenant_id, delivery_id, deleted),
  KEY idx_pre_practice_answer_player (tenant_id, player_id, delivery_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛赛前练习选手作答';
