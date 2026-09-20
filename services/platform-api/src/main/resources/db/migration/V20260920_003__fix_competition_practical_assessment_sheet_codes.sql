-- 实操试卷编号允许重复（例如两套快照都被标记为 A 卷），考评表编号必须使用快照身份区分。
UPDATE competition_practical_assessment_sheet
SET sheet_code = CONCAT(
  CASE
    WHEN INSTR(sheet_code, '-KP-') > 0 THEN LEFT(sheet_code, INSTR(sheet_code, '-KP-') - 1)
    WHEN paper_snapshot_id IS NOT NULL THEN paper_snapshot_id
    ELSE 'PAPER'
  END,
  '-KP-', paper_snapshot_id
)
WHERE paper_snapshot_id IS NOT NULL
  AND (sheet_code IS NULL OR sheet_code NOT LIKE CONCAT('%-KP-', paper_snapshot_id));
