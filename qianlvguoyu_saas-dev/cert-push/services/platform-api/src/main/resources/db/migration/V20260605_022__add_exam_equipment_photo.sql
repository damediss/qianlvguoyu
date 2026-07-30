ALTER TABLE certification_exam_equipment
  ADD COLUMN equipment_photo_file_id BIGINT NULL COMMENT 'equipment photo file id' AFTER equipment_status,
  ADD COLUMN equipment_photo_file_name VARCHAR(300) NULL COMMENT 'equipment photo file name' AFTER equipment_photo_file_id;
