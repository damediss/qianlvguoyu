package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionAssessmentContentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPracticalQuestionAssessmentContentMapper
    extends BaseMapper<CompetitionPracticalQuestionAssessmentContentEntity> {

  @Select("SELECT * FROM competition_practical_question_assessment_content "
      + "WHERE tenant_id = #{tenantId} AND question_id = #{questionId} AND deleted = 0 "
      + "ORDER BY sort_order, id")
  List<CompetitionPracticalQuestionAssessmentContentEntity> selectByQuestion(
      @Param("tenantId") Long tenantId, @Param("questionId") Long questionId);
}
