package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalCandidateAnswerMapper
    extends BaseMapper<CompetitionFormalCandidateAnswerEntity> {

  @Select("""
      SELECT * FROM competition_formal_candidate_answer
      WHERE candidate_exam_id = #{candidateExamId}
        AND question_snapshot_id = #{questionSnapshotId}
        AND deleted = 0
      FOR UPDATE
      """)
  CompetitionFormalCandidateAnswerEntity selectByCandidateQuestionForUpdate(
      @Param("candidateExamId") Long candidateExamId,
      @Param("questionSnapshotId") Long questionSnapshotId);

  @Select("""
      SELECT * FROM competition_formal_candidate_answer
      WHERE candidate_exam_id = #{candidateExamId} AND deleted = 0
      ORDER BY id ASC
      """)
  List<CompetitionFormalCandidateAnswerEntity> selectByCandidateExamId(
      @Param("candidateExamId") Long candidateExamId);

  @Select("""
      SELECT COUNT(*) FROM competition_formal_candidate_answer
      WHERE candidate_exam_id = #{candidateExamId}
        AND is_answered = 1
        AND deleted = 0
      """)
  int countAnswered(@Param("candidateExamId") Long candidateExamId);

  @Select({
      "<script>",
      "SELECT * FROM competition_formal_candidate_answer",
      "WHERE candidate_exam_id = #{candidateExamId}",
      "AND deleted = 0",
      "AND question_snapshot_id IN",
      "<foreach collection='questionSnapshotIds' item='questionSnapshotId' open='(' separator=',' close=')'>",
      "#{questionSnapshotId}",
      "</foreach>",
      "ORDER BY question_snapshot_id ASC",
      "FOR UPDATE",
      "</script>"
  })
  List<CompetitionFormalCandidateAnswerEntity> selectByCandidateQuestionIdsForUpdate(
      @Param("candidateExamId") Long candidateExamId,
      @Param("questionSnapshotIds") List<Long> questionSnapshotIds);

  @Select({
      "<script>",
      "SELECT candidate_exam_id AS candidateExamId, COUNT(*) AS total",
      "FROM competition_formal_candidate_answer",
      "WHERE is_answered = 1 AND deleted = 0",
      "AND candidate_exam_id IN",
      "<foreach collection='candidateExamIds' item='candidateExamId' open='(' separator=',' close=')'>",
      "#{candidateExamId}",
      "</foreach>",
      "GROUP BY candidate_exam_id",
      "</script>"
  })
  List<CompetitionFormalAnsweredCountRow> countAnsweredByCandidateIds(
      @Param("candidateExamIds") List<Long> candidateExamIds);
}
