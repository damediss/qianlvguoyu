package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalCandidateQuestionScoreMapper
    extends BaseMapper<CompetitionFormalCandidateQuestionScoreEntity> {

  @Select("""
      SELECT * FROM competition_formal_candidate_question_score
      WHERE candidate_exam_id = #{candidateExamId} AND deleted = 0
      ORDER BY question_order ASC, id ASC
      """)
  List<CompetitionFormalCandidateQuestionScoreEntity> selectByCandidateExamId(
      @Param("candidateExamId") Long candidateExamId);

  @Select("""
      SELECT * FROM competition_formal_candidate_question_score
      WHERE candidate_result_id = #{candidateResultId} AND deleted = 0
      ORDER BY question_order ASC, id ASC
      """)
  List<CompetitionFormalCandidateQuestionScoreEntity> selectByCandidateResultId(
      @Param("candidateResultId") Long candidateResultId);
}
