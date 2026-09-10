package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalExamAccessSessionMapper
    extends BaseMapper<CompetitionFormalExamAccessSessionEntity> {

  @Select("""
      SELECT * FROM competition_formal_exam_access_session
      WHERE candidate_exam_id = #{candidateExamId}
        AND status = 'active'
        AND deleted = 0
      ORDER BY id ASC
      FOR UPDATE
      """)
  List<CompetitionFormalExamAccessSessionEntity> selectActiveByCandidateExamIdForUpdate(
      @Param("candidateExamId") Long candidateExamId);

  @Select("""
      SELECT * FROM competition_formal_exam_access_session
      WHERE token_fingerprint = #{tokenFingerprint}
        AND deleted = 0
      ORDER BY id ASC
      LIMIT 1
      """)
  CompetitionFormalExamAccessSessionEntity selectByTokenFingerprint(
      @Param("tokenFingerprint") String tokenFingerprint);

  @Select("""
      SELECT COALESCE(MAX(generation), 0) FROM competition_formal_exam_access_session
      WHERE candidate_exam_id = #{candidateExamId}
        AND deleted = 0
      """)
  Integer selectMaxGeneration(@Param("candidateExamId") Long candidateExamId);

  @Update("""
      UPDATE competition_formal_exam_access_session
      SET status = #{toStatus},
          ended_at = #{endedAt},
          ended_reason = #{endedReason},
          updated_at = #{endedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND generation = #{generation}
        AND deleted = 0
      """)
  int markEndedIfGeneration(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("generation") int generation,
      @Param("endedReason") String endedReason,
      @Param("endedAt") LocalDateTime endedAt,
      @Param("updatedBy") Long updatedBy);

  @Update("""
      UPDATE competition_formal_exam_access_session
      SET token_fingerprint = #{tokenFingerprint},
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = 'active'
        AND deleted = 0
      """)
  int updateTokenFingerprint(
      @Param("id") Long id,
      @Param("tokenFingerprint") String tokenFingerprint,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @Select("""
      <script>
      SELECT a.* FROM competition_formal_exam_access_session a
      INNER JOIN (
        SELECT candidate_exam_id, MAX(id) AS id
        FROM competition_formal_exam_access_session
        WHERE deleted = 0
          AND candidate_exam_id IN
          <foreach collection="candidateExamIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY candidate_exam_id
      ) latest ON a.id = latest.id
      WHERE a.deleted = 0
      </script>
      """)
  List<CompetitionFormalExamAccessSessionEntity> selectLatestByCandidateExamIds(
      @Param("candidateExamIds") List<Long> candidateExamIds);
}
