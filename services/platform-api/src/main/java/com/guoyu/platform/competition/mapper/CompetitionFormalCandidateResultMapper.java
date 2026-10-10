package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalCandidateResultMapper
    extends BaseMapper<CompetitionFormalCandidateResultEntity> {

  @Select("""
      SELECT * FROM competition_formal_candidate_result
      WHERE candidate_exam_id = #{candidateExamId} AND current_flag = 1 AND deleted = 0
      LIMIT 1
      """)
  CompetitionFormalCandidateResultEntity selectByCandidateExamId(
      @Param("candidateExamId") Long candidateExamId);

  @Select("""
      SELECT * FROM competition_formal_candidate_result
      WHERE exam_session_id = #{examSessionId} AND current_flag = 1 AND deleted = 0
      ORDER BY id ASC
      """)
  List<CompetitionFormalCandidateResultEntity> selectByExamSessionId(
      @Param("examSessionId") Long examSessionId);

  @Select({
      "<script>",
      "SELECT * FROM competition_formal_candidate_result",
      "WHERE exam_session_id = #{examSessionId} AND current_flag = 1 AND deleted = 0",
      "AND candidate_exam_id IN",
      "<foreach collection='candidateExamIds' item='candidateExamId' open='(' separator=',' close=')'>",
      "#{candidateExamId}",
      "</foreach>",
      "</script>"
  })
  List<CompetitionFormalCandidateResultEntity> selectByExamSessionIdAndCandidateIds(
      @Param("examSessionId") Long examSessionId,
      @Param("candidateExamIds") List<Long> candidateExamIds);

  @Select("""
      SELECT result_status AS status, COUNT(*) AS total
      FROM competition_formal_candidate_result
      WHERE exam_session_id = #{examSessionId}
        AND current_flag = 1 AND deleted = 0
      GROUP BY result_status
      """)
  List<CompetitionFormalStatusCountRow> countStatusByExamSessionId(
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT * FROM competition_formal_candidate_result
      WHERE candidate_exam_id = #{candidateExamId} AND deleted = 0
      ORDER BY revision_no DESC
      """)
  List<CompetitionFormalCandidateResultEntity> selectHistoryByCandidateExamId(
      @Param("candidateExamId") Long candidateExamId);

  @org.apache.ibatis.annotations.Update("""
      UPDATE competition_formal_candidate_result
      SET current_flag = 0, updated_at = #{now}
      WHERE candidate_exam_id = #{candidateExamId} AND current_flag = 1 AND deleted = 0
      """)
  int clearCurrent(@Param("candidateExamId") Long candidateExamId,
      @Param("now") java.time.LocalDateTime now);
}
