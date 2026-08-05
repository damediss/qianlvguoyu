-- COMP-SCORE-01-FIX-03: support profile-first locking and null-stage conflict checks.
-- The version was selected after querying flyway_schema_history: max=20260801.008.
ALTER TABLE competition_theory_attempt
  ADD KEY idx_comp_theory_attempt_profile_status (
    tenant_id, contestant_profile_id, status, deleted
  );
