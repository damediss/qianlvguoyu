package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryPaperQuestionMapper
    extends BaseMapper<CompetitionTheoryPaperQuestionEntity> {

  @Select("SELECT * FROM competition_theory_paper_question WHERE paper_snapshot_id = #{paperId} "
      + "AND deleted = 0 ORDER BY sort_order, id")
  List<CompetitionTheoryPaperQuestionEntity> selectByPaper(@Param("paperId") Long paperId);
}
