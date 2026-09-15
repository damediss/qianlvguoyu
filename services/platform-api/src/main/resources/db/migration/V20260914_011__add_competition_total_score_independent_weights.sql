-- Align the immutable total-score snapshot table with the level-scoped score rule/entity.
-- These columns were added to competition_score_rule in V20260802_003 but were omitted here.
ALTER TABLE competition_total_score_snapshot
  ADD COLUMN simulation_weight DECIMAL(5,2) NULL AFTER theory_weight,
  ADD COLUMN practical_weight DECIMAL(5,2) NULL AFTER simulation_weight;
