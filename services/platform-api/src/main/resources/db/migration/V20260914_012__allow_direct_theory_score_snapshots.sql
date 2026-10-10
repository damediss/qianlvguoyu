-- Theory-only stages use the score already calculated from the immutable question snapshot.
-- Keep score_rule_id nullable for those snapshots while mixed-content stages continue to
-- require a published rule in the application layer.
ALTER TABLE competition_score_source
  MODIFY COLUMN score_rule_id BIGINT NULL;

ALTER TABLE competition_total_score_snapshot
  MODIFY COLUMN score_rule_id BIGINT NULL,
  MODIFY COLUMN theory_source_id BIGINT NULL,
  MODIFY COLUMN simulation_source_id BIGINT NULL,
  MODIFY COLUMN practical_source_id BIGINT NULL;

ALTER TABLE competition_ranking_snapshot
  MODIFY COLUMN score_rule_id BIGINT NULL;
