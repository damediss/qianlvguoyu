package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPracticalQuestionDetailMapper
    extends BaseMapper<CompetitionPracticalQuestionDetailEntity> {

  @Select("SELECT * FROM competition_practical_question_detail "
      + "WHERE tenant_id = #{tenantId} AND question_id = #{questionId} AND deleted = 0 LIMIT 1")
  CompetitionPracticalQuestionDetailEntity selectByQuestion(
      @Param("tenantId") Long tenantId, @Param("questionId") Long questionId);
}
