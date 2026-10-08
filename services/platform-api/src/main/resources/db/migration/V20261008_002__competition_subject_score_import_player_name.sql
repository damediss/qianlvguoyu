-- Keep the imported player name so score sheets can show and check 姓名.

ALTER TABLE competition_subject_score_import_row
  ADD COLUMN player_name VARCHAR(64) NULL COMMENT '导入表中的选手姓名' AFTER id_card_no;
