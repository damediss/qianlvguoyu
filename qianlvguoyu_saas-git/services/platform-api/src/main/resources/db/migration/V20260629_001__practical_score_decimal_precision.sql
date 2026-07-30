ALTER TABLE certification_practical_question_result
  MODIFY final_score DECIMAL(10,1) NULL COMMENT '保留1位小数的试题最终分';

ALTER TABLE certification_practical_candidate_result
  MODIFY total_score DECIMAL(10,1) NULL COMMENT '所有试题最终分之和，保留1位小数';
