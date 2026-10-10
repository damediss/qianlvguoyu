package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionMakeExamPaperQuestionMapper
    extends BaseMapper<CompetitionMakeExamPaperQuestionEntity> {

  @Select("SELECT * FROM competition_make_exam_paper_question "
      + "WHERE tenant_id = #{tenantId} AND paper_snapshot_id = #{paperId} AND deleted = 0 "
      + "ORDER BY sort_order, id")
  List<CompetitionMakeExamPaperQuestionEntity> selectByPaper(
      @Param("tenantId") Long tenantId, @Param("paperId") Long paperId);
}
