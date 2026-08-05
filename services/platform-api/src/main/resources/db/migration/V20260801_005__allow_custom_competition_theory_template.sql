ALTER TABLE competition_score_rule
  DROP CHECK chk_competition_score_rule_template;

ALTER TABLE competition_score_rule
  ADD CONSTRAINT chk_competition_score_rule_template CHECK (
    theory_template IS NULL
    OR theory_template IN ('THEORY_90', 'THEORY_120', 'CUSTOM')
  );
