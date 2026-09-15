package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalCandidateExamMapper
    extends BaseMapper<CompetitionFormalCandidateExamEntity> {

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT c.tenant_id
      FROM competition_formal_candidate_exam c
      LEFT JOIN competition_formal_scoring_task t
        ON t.tenant_id = c.tenant_id
       AND t.candidate_exam_id = c.id
       AND t.result_revision_no = 1
       AND t.deleted = 0
      WHERE c.deleted = 0
        AND c.status IN ('submitted', 'auto_submitted', 'forced_submitted', 'absent')
        AND t.id IS NULL
      """)
  List<Long> selectMissingInitialScoringTenantIds();

  @Select("""
      SELECT c.*
      FROM competition_formal_candidate_exam c
      LEFT JOIN competition_formal_scoring_task t
        ON t.tenant_id = c.tenant_id
       AND t.candidate_exam_id = c.id
       AND t.result_revision_no = 1
       AND t.deleted = 0
      WHERE c.deleted = 0
        AND c.status IN ('submitted', 'auto_submitted', 'forced_submitted', 'absent')
        AND t.id IS NULL
      ORDER BY c.id
      LIMIT #{limit}
      """)
  List<CompetitionFormalCandidateExamEntity> selectMissingInitialScoringCandidates(
      @Param("limit") int limit);

  @Select("""
      SELECT * FROM competition_formal_candidate_exam
      WHERE id = #{id} AND deleted = 0
      FOR UPDATE
      """)
  CompetitionFormalCandidateExamEntity selectByIdForUpdate(@Param("id") Long id);

  @Select({
      "<script>",
      "SELECT * FROM competition_formal_candidate_exam",
      "WHERE exam_session_id = #{examSessionId} AND deleted = 0",
      "AND contestant_profile_id IN",
      "<foreach collection='profileIds' item='profileId' open='(' separator=',' close=')'>",
      "#{profileId}",
      "</foreach>",
      "ORDER BY id ASC",
      "</script>"
  })
  List<CompetitionFormalCandidateExamEntity> selectBySessionAndProfileIds(
      @Param("examSessionId") Long examSessionId,
      @Param("profileIds") List<Long> profileIds);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          enter_time = #{enterTime},
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markEnterIfWaiting(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("enterTime") LocalDateTime enterTime,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          start_time = #{startTime},
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE exam_session_id = #{examSessionId}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markReadyToRunning(
      @Param("examSessionId") Long examSessionId,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("startTime") LocalDateTime startTime,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET answered_count = #{answeredCount},
          last_active_time = #{now},
          updated_at = #{now},
          updated_by = #{updatedBy}
      WHERE id = #{id} AND deleted = 0
      """)
  int updateAnsweredCount(
      @Param("id") Long id,
      @Param("answeredCount") int answeredCount,
      @Param("now") LocalDateTime now,
      @Param("updatedBy") Long updatedBy);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          submit_time = #{submitTime},
          submit_type = #{submitType},
          answered_count = #{answeredCount},
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markSubmittedIfRunning(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("submitTime") LocalDateTime submitTime,
      @Param("submitType") String submitType,
      @Param("answeredCount") int answeredCount,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          submit_time = #{submitTime},
          submit_type = #{submitType},
          answered_count = #{answeredCount},
          force_submit_reason = #{forceSubmitReason},
          force_submitted_by = #{forceSubmittedBy},
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markForcedIfRunning(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("submitTime") LocalDateTime submitTime,
      @Param("submitType") String submitType,
      @Param("answeredCount") int answeredCount,
      @Param("forceSubmitReason") String forceSubmitReason,
      @Param("forceSubmittedBy") Long forceSubmittedBy,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @Select("""
      SELECT id FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND status = #{status}
        AND deleted = 0
      ORDER BY id ASC
      """)
  List<Long> selectIdsBySessionAndStatus(
      @Param("examSessionId") Long examSessionId,
      @Param("status") String status);

  @Select("""
      SELECT id FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND status = #{status}
        AND deleted = 0
      ORDER BY id ASC
      LIMIT #{limit}
      """)
  List<Long> selectIdsBySessionAndStatusLimit(
      @Param("examSessionId") Long examSessionId,
      @Param("status") String status,
      @Param("limit") int limit);

  @Select("""
      SELECT * FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND player_id = #{playerId}
        AND deleted = 0
      ORDER BY id ASC
      """)
  List<CompetitionFormalCandidateExamEntity> selectBySessionAndPlayerId(
      @Param("examSessionId") Long examSessionId,
      @Param("playerId") Long playerId);

  @Select("""
      SELECT c.*
      FROM competition_formal_candidate_exam c
      INNER JOIN competition_formal_exam_session s
        ON s.id = c.exam_session_id
       AND s.deleted = 0
       AND s.status IN ('ready', 'running')
      WHERE c.player_id = #{playerId}
        AND c.deleted = 0
        AND c.status IN ('waiting_enter', 'ready', 'running')
      ORDER BY c.id ASC
      """)
  List<CompetitionFormalCandidateExamEntity> selectJoinableByPlayerId(
      @Param("playerId") Long playerId);

  @Select("""
      SELECT status AS status, COUNT(*) AS total
      FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND deleted = 0
      GROUP BY status
      """)
  List<CompetitionFormalStatusCountRow> countStatusByExamSessionId(
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT paper_snapshot_id AS paperSnapshotId, COUNT(*) AS total
      FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND deleted = 0
      GROUP BY paper_snapshot_id
      """)
  List<CompetitionFormalPaperCandidateCountRow> countPaperCandidatesByExamSessionId(
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT COUNT(*) FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND status = #{status}
        AND deleted = 0
      """)
  int countBySessionAndStatus(
      @Param("examSessionId") Long examSessionId,
      @Param("status") String status);

  @Select("""
      SELECT * FROM competition_formal_candidate_exam
      WHERE exam_session_id = #{examSessionId}
        AND deleted = 0
      ORDER BY id ASC
      """)
  List<CompetitionFormalCandidateExamEntity> selectByExamSessionId(
      @Param("examSessionId") Long examSessionId);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          absent_time = #{absentTime},
          absent_reason = #{absentReason},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markAbsentIfWaitingEnter(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("absentTime") LocalDateTime absentTime,
      @Param("absentReason") String absentReason,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE competition_formal_candidate_exam
      SET status = #{toStatus},
          absent_time = #{absentTime},
          absent_reason = #{absentReason},
          updated_at = #{updatedAt}
      WHERE exam_session_id = #{examSessionId}
        AND status = #{fromStatus}
        AND deleted = 0
      """)
  int markAbsentIfWaitingEnterBySession(
      @Param("examSessionId") Long examSessionId,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("absentTime") LocalDateTime absentTime,
      @Param("absentReason") String absentReason,
      @Param("updatedAt") LocalDateTime updatedAt);
}
