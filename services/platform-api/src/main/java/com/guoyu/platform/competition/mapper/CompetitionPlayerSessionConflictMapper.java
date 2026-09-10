package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPlayerSessionConflictMapper {

  @InterceptorIgnore(tenantLine = "true")
  @Select({
      "<script>",
      "SELECT source, event_name, work_type_name, session_name, start_at, end_at",
      "FROM (",
      "  SELECT 'theory' AS source, e.name AS event_name, wt.display_name AS work_type_name,",
      "         COALESCE(s.session_name, s.session_code) AS session_name,",
      "         s.start_at AS start_at, s.end_at AS end_at",
      "  FROM competition_theory_candidate_assignment a",
      "  JOIN competition_theory_exam_session s",
      "    ON s.id = a.session_id AND s.tenant_id = a.tenant_id AND s.deleted = 0",
      "  JOIN competition_theory_schedule_version v",
      "    ON v.id = a.schedule_version_id AND v.tenant_id = a.tenant_id",
      "   AND v.status IN ('draft', 'published') AND v.deleted = 0",
      "  JOIN competition_event e",
      "    ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0",
      "  LEFT JOIN competition_event_work_type wt",
      "    ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0",
      "  WHERE a.tenant_id = #{tenantId}",
      "    AND a.player_id = #{playerId}",
      "    AND a.status IN ('draft', 'published') AND a.deleted = 0",
      "    AND s.start_at IS NOT NULL AND s.end_at IS NOT NULL",
      "    AND s.start_at &lt; #{endAt} AND s.end_at &gt; #{startAt}",
      "<if test='excludeTheoryScheduleVersionId != null'>",
      "    AND a.schedule_version_id &lt;&gt; #{excludeTheoryScheduleVersionId}",
      "</if>",
      "  UNION ALL",
      "  SELECT 'practical' AS source, e.name AS event_name, wt.display_name AS work_type_name,",
      "         COALESCE(s.session_name, s.session_code) AS session_name,",
      "         s.start_at AS start_at, s.end_at AS end_at",
      "  FROM competition_practical_assignment a",
      "  JOIN competition_practical_session s",
      "    ON s.id = a.session_id AND s.tenant_id = a.tenant_id AND s.deleted = 0",
      "  JOIN competition_event_member m",
      "    ON m.id = a.contestant_member_id AND m.tenant_id = a.tenant_id AND m.deleted = 0",
      "  JOIN competition_event e",
      "    ON e.id = s.event_id AND e.tenant_id = s.tenant_id AND e.deleted = 0",
      "  LEFT JOIN competition_event_work_type wt",
      "    ON wt.id = s.event_work_type_id AND wt.tenant_id = s.tenant_id AND wt.deleted = 0",
      "  WHERE a.tenant_id = #{tenantId}",
      "    AND m.person_id = #{playerId}",
      "    AND a.status IN ('assigned', 'in_progress', 'completed') AND a.deleted = 0",
      "    AND s.status IN ('draft', 'published', 'in_progress')",
      "    AND s.start_at IS NOT NULL AND s.end_at IS NOT NULL",
      "    AND s.start_at &lt; #{endAt} AND s.end_at &gt; #{startAt}",
      "<if test='excludePracticalSessionId != null'>",
      "    AND s.id &lt;&gt; #{excludePracticalSessionId}",
      "</if>",
      "  UNION ALL",
      "  SELECT 'formal' AS source, e.name AS event_name, wt.display_name AS work_type_name,",
      "         '正式比赛' AS session_name,",
      "         COALESCE(s.actual_start_time, s.planned_start_time) AS start_at,",
      "         DATE_ADD(COALESCE(s.actual_start_time, s.planned_start_time), INTERVAL s.duration_seconds SECOND) AS end_at",
      "  FROM competition_formal_candidate_exam c",
      "  JOIN competition_formal_exam_session s",
      "    ON s.id = c.exam_session_id AND s.tenant_id = c.tenant_id AND s.deleted = 0",
      "  JOIN competition_event e",
      "    ON e.id = s.event_id AND e.tenant_id = s.tenant_id AND e.deleted = 0",
      "  LEFT JOIN competition_event_work_type wt",
      "    ON wt.id = s.event_work_type_id AND wt.tenant_id = s.tenant_id AND wt.deleted = 0",
      "  WHERE c.tenant_id = #{tenantId}",
      "    AND c.player_id = #{playerId}",
      "    AND c.deleted = 0",
      "    AND s.status IN ('preparing', 'ready', 'running')",
      "    AND COALESCE(s.actual_start_time, s.planned_start_time) IS NOT NULL",
      "    AND COALESCE(s.actual_start_time, s.planned_start_time) &lt; #{endAt}",
      "    AND DATE_ADD(COALESCE(s.actual_start_time, s.planned_start_time), INTERVAL s.duration_seconds SECOND) &gt; #{startAt}",
      "<if test='excludeFormalExamSessionId != null'>",
      "    AND s.id &lt;&gt; #{excludeFormalExamSessionId}",
      "</if>",
      ") conflicts",
      "ORDER BY start_at, session_name",
      "LIMIT 1",
      "</script>"
  })
  CompetitionPlayerSessionConflictRow findFirstOverlap(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt,
      @Param("excludeTheoryScheduleVersionId") Long excludeTheoryScheduleVersionId,
      @Param("excludePracticalSessionId") Long excludePracticalSessionId,
      @Param("excludeFormalExamSessionId") Long excludeFormalExamSessionId);

  default CompetitionPlayerSessionConflictRow findFirstOverlap(
      Long tenantId, Long playerId, LocalDateTime startAt, LocalDateTime endAt) {
    return findFirstOverlap(tenantId, playerId, startAt, endAt, null, null, null);
  }
}
