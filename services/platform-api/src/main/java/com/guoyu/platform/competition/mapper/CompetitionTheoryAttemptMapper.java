package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionTheoryAttemptMapper extends BaseMapper<CompetitionTheoryAttemptEntity> {

  @Select("SELECT * FROM competition_theory_attempt WHERE assignment_id = #{assignmentId} "
      + "AND deleted = 0 LIMIT 1 FOR UPDATE")
  CompetitionTheoryAttemptEntity selectForUpdateByAssignment(@Param("assignmentId") Long assignmentId);

  @Select("SELECT * FROM competition_theory_attempt WHERE id = #{attemptId} "
      + "AND deleted = 0 FOR UPDATE")
  CompetitionTheoryAttemptEntity selectForUpdateById(@Param("attemptId") Long attemptId);

  @Select("""
      SELECT * FROM competition_theory_attempt
      WHERE tenant_id = #{tenantId} AND contestant_profile_id = #{contestantProfileId}
        AND status NOT IN ('submitted_manual', 'submitted_auto', 'submitted_forced', 'absent')
        AND deleted = 0
      ORDER BY id
      FOR UPDATE
      """)
  List<CompetitionTheoryAttemptEntity> selectNonTerminalForUpdateByProfile(
      @Param("tenantId") Long tenantId,
      @Param("contestantProfileId") Long contestantProfileId);

  @Select("""
      SELECT * FROM competition_theory_attempt
      WHERE player_id = #{playerId} AND status NOT IN
        ('submitted_manual', 'submitted_auto', 'submitted_forced', 'absent')
        AND deleted = 0
      ORDER BY created_at DESC LIMIT 1
      """)
  CompetitionTheoryAttemptEntity selectCurrentByPlayer(@Param("playerId") Long playerId);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = 'in_progress', started_at = COALESCE(started_at, #{now}),
          session_state = 'active', last_session_event_at = #{now},
          absence_flag = 0, lock_version = lock_version + 1, updated_at = #{now}
      WHERE id = #{attemptId} AND status IN ('waiting', 'ready') AND deleted = 0
      """)
  int start(@Param("attemptId") Long attemptId, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = 'submitting', submit_type = #{submitType}, submit_request_id = #{requestId},
          forced_by = #{forcedBy}, force_reason = #{reason}, lock_version = lock_version + 1,
          updated_at = #{now}
      WHERE id = #{attemptId} AND status = 'in_progress' AND deleted = 0
      """)
  int claimSubmission(
      @Param("attemptId") Long attemptId,
      @Param("submitType") String submitType,
      @Param("requestId") String requestId,
      @Param("forcedBy") Long forcedBy,
      @Param("reason") String reason,
      @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = #{status}, submitted_at = #{now}, total_score = #{totalScore},
          score_source_id = #{scoreSourceId}, session_state = 'closed',
          last_session_event_at = #{now}, lock_version = lock_version + 1, updated_at = #{now}
      WHERE id = #{attemptId} AND status = 'submitting' AND deleted = 0
      """)
  int finishSubmission(
      @Param("attemptId") Long attemptId,
      @Param("status") String status,
      @Param("totalScore") java.math.BigDecimal totalScore,
      @Param("scoreSourceId") Long scoreSourceId,
      @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = 'provisional_absent', absence_flag = 1,
          session_state = 'closed', last_session_event_at = #{now},
          provisional_absent_at = COALESCE(provisional_absent_at, #{now}),
          lock_version = lock_version + 1, updated_at = #{now}
      WHERE id = #{attemptId} AND status IN ('waiting', 'ready') AND deleted = 0
      """)
  int markProvisionalAbsent(@Param("attemptId") Long attemptId, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = 'ready', absence_flag = 0, restore_count = restore_count + 1,
          session_state = 'offline', last_session_event_at = #{now},
          last_restored_at = #{now}, lock_version = lock_version + 1, updated_at = #{now}
      WHERE id = #{attemptId} AND status = 'provisional_absent' AND deleted = 0
      """)
  int restoreAbsence(@Param("attemptId") Long attemptId, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_attempt
      SET status = 'absent', absence_flag = 1, submit_type = 'absent', submitted_at = #{now},
          absence_finalized_at = #{now}, total_score = 0, score_source_id = #{scoreSourceId},
          session_state = 'closed', last_session_event_at = #{now},
          lock_version = lock_version + 1, updated_at = #{now}
      WHERE id = #{attemptId} AND status IN ('waiting', 'ready', 'provisional_absent')
        AND deleted = 0
      """)
  int finishAbsent(
      @Param("attemptId") Long attemptId,
      @Param("scoreSourceId") Long scoreSourceId,
      @Param("now") LocalDateTime now);

  @Select("""
      SELECT * FROM competition_theory_attempt
      WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY id
      """)
  List<CompetitionTheoryAttemptEntity> selectBySession(@Param("sessionId") Long sessionId);

  @Update("""
      UPDATE competition_theory_attempt
      SET session_state = #{sessionState}, last_session_event_at = #{now}, updated_at = #{now}
      WHERE id = #{attemptId} AND deleted = 0
      """)
  int updateSessionState(
      @Param("attemptId") Long attemptId,
      @Param("sessionState") String sessionState,
      @Param("now") LocalDateTime now);
}
