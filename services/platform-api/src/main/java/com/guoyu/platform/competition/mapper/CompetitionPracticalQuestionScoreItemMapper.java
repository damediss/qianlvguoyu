package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPracticalQuestionScoreItemMapper
    extends BaseMapper<CompetitionPracticalQuestionScoreItemEntity> {

  @Select("SELECT * FROM competition_practical_question_score_item "
      + "WHERE tenant_id = #{tenantId} AND question_id = #{questionId} AND deleted = 0 "
      + "ORDER BY item_no, id")
  List<CompetitionPracticalQuestionScoreItemEntity> selectByQuestion(
      @Param("tenantId") Long tenantId, @Param("questionId") Long questionId);
}
