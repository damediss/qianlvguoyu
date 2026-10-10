package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryCandidateAssignmentEntity;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryCandidateAssignmentMapper
    extends BaseMapper<CompetitionTheoryCandidateAssignmentEntity> {

  @Select("""
      SELECT COUNT(1) FROM competition_theory_candidate_assignment
      WHERE tenant_id = #{tenantId} AND event_id = #{eventId}
        AND contestant_profile_id = #{contestantProfileId}
        AND status IN ('draft', 'published') AND deleted = 0
      """)
  long countScopeChangeBlockers(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("contestantProfileId") Long contestantProfileId);

  @Select("""
      SELECT p.id AS contestant_profile_id, p.member_id AS contestant_member_id,
             p.registration_id, r.player_id, p.contestant_no,
             i.name AS contestant_name, o.name AS org_name
      FROM competition_contestant_profile p
      JOIN competition_event_member m
        ON m.id = p.member_id AND m.tenant_id = p.tenant_id
        AND m.event_id = #{eventId} AND m.role_type = 'contestant'
        AND m.status = 'active' AND m.deleted = 0
      JOIN competition_registration r
        ON r.id = p.registration_id AND r.tenant_id = p.tenant_id
        AND r.event_id = #{eventId}
        AND r.status IN ('eligible_unactivated', 'eligible_activated')
        AND r.deleted = 0
      JOIN competition_roster_item i
        ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0
      LEFT JOIN sys_org o
        ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0
      WHERE p.tenant_id = #{tenantId}
        AND (p.current_stage_id IS NULL OR p.current_stage_id = #{stageId})
        AND p.event_work_type_id = #{eventWorkTypeId}
        AND p.work_type_match_status = 'matched'
        AND p.deleted = 0
      ORDER BY p.id
      """)
  List<CompetitionTheoryCandidateRow> selectEligibleCandidates(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  @Select("""
      SELECT a.id AS assignment_id, a.schedule_version_id, a.event_id, a.player_id, a.session_id,
             e.name AS event_name, wt.display_name AS work_type_name,
             s.session_name, s.start_at, s.end_at,
             sr.room_name_snapshot AS room_name, sr.address_snapshot AS address,
             a.seat_no, p.contestant_no, i.name AS contestant_name, o.name AS org_name
      FROM competition_theory_candidate_assignment a
      JOIN competition_event e
        ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0
      JOIN competition_event_work_type wt
        ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0
      JOIN competition_theory_exam_session s
        ON s.id = a.session_id AND s.tenant_id = a.tenant_id AND s.deleted = 0
      JOIN competition_theory_session_room sr
        ON sr.id = a.session_room_id AND sr.tenant_id = a.tenant_id AND sr.deleted = 0
      JOIN competition_contestant_profile p
        ON p.id = a.contestant_profile_id AND p.tenant_id = a.tenant_id AND p.deleted = 0
      JOIN competition_registration r
        ON r.id = a.registration_id AND r.tenant_id = a.tenant_id AND r.deleted = 0
      JOIN competition_roster_item i
        ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0
      LEFT JOIN sys_org o
        ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0
      WHERE a.tenant_id = #{tenantId}
        AND a.schedule_version_id = #{scheduleVersionId} AND a.deleted = 0
      ORDER BY s.start_at, s.session_code, sr.sort_order, a.seat_no
      """)
  List<CompetitionTheoryRosterRow> selectRosterRows(
      @Param("tenantId") Long tenantId,
      @Param("scheduleVersionId") Long scheduleVersionId);

  @Select("""
      SELECT a.id AS assignment_id, a.schedule_version_id, a.event_id, a.player_id, a.session_id,
             e.name AS event_name, wt.display_name AS work_type_name,
             s.session_name, s.start_at, s.end_at,
             sr.room_name_snapshot AS room_name, sr.address_snapshot AS address,
             a.seat_no, p.contestant_no, i.name AS contestant_name, o.name AS org_name
      FROM competition_theory_candidate_assignment a
      JOIN competition_theory_schedule_version v
        ON v.id = a.schedule_version_id AND v.tenant_id = a.tenant_id
        AND v.status = 'published' AND v.deleted = 0
      JOIN competition_event e
        ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0
      JOIN competition_event_work_type wt
        ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0
      JOIN competition_theory_exam_session s
        ON s.id = a.session_id AND s.tenant_id = a.tenant_id
        AND s.status = 'published' AND s.deleted = 0
      JOIN competition_theory_session_room sr
        ON sr.id = a.session_room_id AND sr.tenant_id = a.tenant_id AND sr.deleted = 0
      JOIN competition_contestant_profile p
        ON p.id = a.contestant_profile_id AND p.tenant_id = a.tenant_id AND p.deleted = 0
      JOIN competition_registration r
        ON r.id = a.registration_id AND r.tenant_id = a.tenant_id
        AND r.status = 'eligible_activated' AND r.deleted = 0
      JOIN competition_roster_item i
        ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0
      LEFT JOIN sys_org o
        ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0
      WHERE a.tenant_id = #{tenantId} AND a.player_id = #{playerId}
        AND a.status = 'published' AND a.deleted = 0
      ORDER BY s.start_at, a.id
      """)
  List<CompetitionTheoryRosterRow> selectPublishedByPlayer(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT a.id AS assignment_id, a.tenant_id, a.schedule_version_id, a.session_id,
             s.paper_snapshot_id, a.event_id, a.stage_id, a.event_work_type_id,
             a.contestant_profile_id, a.contestant_member_id, a.player_id,
             s.start_at, s.end_at, s.session_name, s.execution_status,
             e.name AS event_name, wt.display_name AS work_type_name,
             sr.room_name_snapshot AS room_name, sr.address_snapshot AS address, a.seat_no,
             p.score_rule_id, p.question_count
      FROM competition_theory_candidate_assignment a
      JOIN competition_theory_schedule_version v
        ON v.id = a.schedule_version_id AND v.tenant_id = a.tenant_id
        AND v.status = 'published' AND v.deleted = 0
      JOIN competition_theory_exam_session s
        ON s.id = a.session_id AND s.tenant_id = a.tenant_id
        AND s.status = 'published' AND s.deleted = 0
      JOIN competition_theory_paper_snapshot p
        ON p.id = s.paper_snapshot_id AND p.tenant_id = a.tenant_id
        AND p.status = 'published' AND p.deleted = 0
      JOIN competition_event e
        ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0
      JOIN competition_event_work_type wt
        ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0
      JOIN competition_theory_session_room sr
        ON sr.id = a.session_room_id AND sr.tenant_id = a.tenant_id AND sr.deleted = 0
      JOIN competition_registration r
        ON r.id = a.registration_id AND r.tenant_id = a.tenant_id
        AND r.status = 'eligible_activated' AND r.deleted = 0
      WHERE a.player_id = #{playerId} AND a.status = 'published' AND a.deleted = 0
        AND s.start_at <= DATE_ADD(#{now}, INTERVAL 30 MINUTE)
        AND s.end_at > #{now}
      ORDER BY s.start_at, a.id
      """)
  List<CompetitionTheoryExamAssignmentRow> selectExamLoginCandidatesGlobally(
      @Param("playerId") Long playerId,
      @Param("now") java.time.LocalDateTime now);

  @Select("""
      SELECT a.id AS assignment_id, a.tenant_id, a.schedule_version_id, a.session_id,
             s.paper_snapshot_id, a.event_id, a.stage_id, a.event_work_type_id,
             a.contestant_profile_id, a.contestant_member_id, a.player_id,
             s.start_at, s.end_at, s.session_name, s.execution_status,
             e.name AS event_name, wt.display_name AS work_type_name,
             sr.room_name_snapshot AS room_name, sr.address_snapshot AS address, a.seat_no,
             p.score_rule_id, p.question_count
      FROM competition_theory_candidate_assignment a
      JOIN competition_theory_exam_session s
        ON s.id = a.session_id AND s.tenant_id = a.tenant_id AND s.deleted = 0
      JOIN competition_theory_paper_snapshot p
        ON p.id = s.paper_snapshot_id AND p.tenant_id = a.tenant_id AND p.deleted = 0
      JOIN competition_event e
        ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0
      JOIN competition_event_work_type wt
        ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0
      JOIN competition_theory_session_room sr
        ON sr.id = a.session_room_id AND sr.tenant_id = a.tenant_id AND sr.deleted = 0
      WHERE a.tenant_id = #{tenantId} AND a.id = #{assignmentId} AND a.deleted = 0
      """)
  CompetitionTheoryExamAssignmentRow selectExamAssignment(
      @Param("tenantId") Long tenantId,
      @Param("assignmentId") Long assignmentId);

  @Select("""
      SELECT a.id AS assignment_id, a.tenant_id, a.schedule_version_id, a.session_id,
             s.paper_snapshot_id, a.event_id, a.stage_id, a.event_work_type_id,
             a.contestant_profile_id, a.contestant_member_id, a.player_id,
             s.start_at, s.end_at, s.session_name, s.execution_status,
             e.name AS event_name, wt.display_name AS work_type_name,
             sr.room_name_snapshot AS room_name, sr.address_snapshot AS address, a.seat_no,
             p.score_rule_id, p.question_count
      FROM competition_theory_candidate_assignment a
      JOIN competition_theory_exam_session s
        ON s.id = a.session_id AND s.tenant_id = a.tenant_id AND s.deleted = 0
      JOIN competition_theory_paper_snapshot p
        ON p.id = s.paper_snapshot_id AND p.tenant_id = a.tenant_id AND p.deleted = 0
      JOIN competition_event e
        ON e.id = a.event_id AND e.tenant_id = a.tenant_id AND e.deleted = 0
      JOIN competition_event_work_type wt
        ON wt.id = a.event_work_type_id AND wt.tenant_id = a.tenant_id AND wt.deleted = 0
      JOIN competition_theory_session_room sr
        ON sr.id = a.session_room_id AND sr.tenant_id = a.tenant_id AND sr.deleted = 0
      WHERE a.tenant_id = #{tenantId} AND a.session_id = #{sessionId}
        AND a.status = 'published' AND a.player_id IS NOT NULL AND a.deleted = 0
      ORDER BY a.id
      """)
  List<CompetitionTheoryExamAssignmentRow> selectPublishedExamAssignmentsBySession(
      @Param("tenantId") Long tenantId,
      @Param("sessionId") Long sessionId);
}
