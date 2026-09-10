ALTER TABLE competition_formal_candidate_exam
  ADD COLUMN absent_time DATETIME NULL,
  ADD COLUMN absent_reason VARCHAR(64) NULL;

ALTER TABLE competition_formal_candidate_exam
  DROP CHECK chk_comp_formal_candidate_exam_status;

ALTER TABLE competition_formal_candidate_exam
  ADD CONSTRAINT chk_comp_formal_candidate_exam_status CHECK (
    status IN (
      'waiting_enter', 'ready', 'running',
      'submitted', 'auto_submitted', 'forced_submitted',
      'absent'
    )
  );
