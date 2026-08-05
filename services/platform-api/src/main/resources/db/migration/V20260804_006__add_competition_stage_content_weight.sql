ALTER TABLE competition_stage_content
  ADD COLUMN weight DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER status;
