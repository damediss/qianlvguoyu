package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerSaveRequestEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryAnswerSaveRequestMapper
    extends BaseMapper<CompetitionTheoryAnswerSaveRequestEntity> {
  @Select("SELECT * FROM competition_theory_answer_save_request WHERE attempt_id = #{attemptId} "
      + "AND request_id = #{requestId} AND deleted = 0 LIMIT 1")
  CompetitionTheoryAnswerSaveRequestEntity selectByRequest(
      @Param("attemptId") Long attemptId,
      @Param("requestId") String requestId);
}
