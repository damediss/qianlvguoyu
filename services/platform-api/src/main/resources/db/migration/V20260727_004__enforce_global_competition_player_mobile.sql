-- A phone number is the competition account, so only one non-deleted player identity may own it
-- across every tenant. Fail before changing the schema so administrators can resolve conflicts.
DELIMITER $$

CREATE PROCEDURE assert_competition_player_active_mobile_unique()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM competition_player_identity
    WHERE deleted = 0
    GROUP BY mobile_lookup_hash
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Competition player mobile hash is duplicated across tenants; resolve active duplicate phone numbers before migration';
  END IF;
END$$

DELIMITER ;

CALL assert_competition_player_active_mobile_unique();
DROP PROCEDURE assert_competition_player_active_mobile_unique;

-- MySQL has no partial unique index. The generated value is NULL for logical-deleted rows,
-- allowing their historical records to remain while globally enforcing one active phone hash.
ALTER TABLE competition_player_identity
  ADD COLUMN active_mobile_lookup_hash CHAR(64)
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN mobile_lookup_hash ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_competition_player_identity_active_mobile_global (active_mobile_lookup_hash);
