ALTER TABLE certification_task_exam_staff
  ADD COLUMN is_leader INT NOT NULL DEFAULT 0 COMMENT '是否组长 0-否 1-是' AFTER status;
