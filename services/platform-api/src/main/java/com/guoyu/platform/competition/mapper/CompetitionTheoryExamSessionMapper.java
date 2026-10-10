package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.List;

public interface CompetitionTheoryExamSessionMapper
    extends BaseMapper<CompetitionTheoryExamSessionEntity> {

  @Select("""
      SELECT COUNT(1)
      FROM competition_theory_session_room sr
      JOIN competition_theory_exam_session s
        ON s.id = sr.session_id AND s.tenant_id = sr.tenant_id AND s.deleted = 0
      JOIN competition_theory_schedule_version v
        ON v.id = s.schedule_version_id AND v.tenant_id = s.tenant_id
        AND v.status IN ('draft', 'published') AND v.deleted = 0
      WHERE sr.tenant_id = #{tenantId} AND sr.room_id = #{roomId} AND sr.deleted = 0
        AND s.id <> #{excludedSessionId}
        AND s.start_at < #{endAt} AND s.end_at > #{startAt}
      """)
  long countRoomTimeConflicts(
      @Param("tenantId") Long tenantId,
      @Param("roomId") Long roomId,
      @Param("excludedSessionId") Long excludedSessionId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  @Select("""
      SELECT COUNT(1)
      FROM competition_theory_candidate_assignment a
      JOIN competition_theory_exam_session assigned_session
        ON assigned_session.id = a.session_id
        AND assigned_session.tenant_id = a.tenant_id
        AND assigned_session.deleted = 0
      JOIN competition_theory_schedule_version v
        ON v.id = a.schedule_version_id AND v.tenant_id = a.tenant_id
        AND v.status IN ('draft', 'published') AND v.deleted = 0
      WHERE a.tenant_id = #{tenantId}
        AND a.player_id = #{playerId}
        AND a.schedule_version_id <> #{scheduleVersionId}
        AND a.status IN ('draft', 'published') AND a.deleted = 0
        AND assigned_session.start_at < #{endAt}
        AND assigned_session.end_at > #{startAt}
      """)
  long countCandidateTimeConflicts(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("scheduleVersionId") Long scheduleVersionId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  @Update("""
      UPDATE competition_theory_exam_session
      SET execution_status = 'open', opened_at = COALESCE(opened_at, #{now}), updated_at = #{now}
      WHERE id = #{sessionId} AND status = 'published' AND execution_status = 'scheduled'
        AND start_at <= #{now} AND end_at > #{now} AND deleted = 0
      """)
  int openIfDue(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_theory_exam_session
      SET execution_status = 'closed', closed_at = COALESCE(closed_at, #{now}), updated_at = #{now}
      WHERE id = #{sessionId} AND status = 'published' AND execution_status <> 'closed'
        AND end_at <= #{now} AND deleted = 0
      """)
  int closeIfDue(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT tenant_id
      FROM competition_theory_exam_session
      WHERE status = 'published' AND deleted = 0
        AND (execution_status <> 'closed' OR end_at <= #{now})
        AND start_at <= DATE_ADD(#{now}, INTERVAL 30 MINUTE)
      """)
  List<Long> selectExecutionTenantIds(@Param("now") LocalDateTime now);

  @Select("""
      SELECT * FROM competition_theory_exam_session
      WHERE status = 'published' AND deleted = 0
        AND start_at <= DATE_ADD(#{now}, INTERVAL 30 MINUTE)
        AND (execution_status <> 'closed' OR end_at <= #{now})
      ORDER BY start_at, id
      """)
  List<CompetitionTheoryExamSessionEntity> selectSessionsForExecution(
      @Param("now") LocalDateTime now);
}
