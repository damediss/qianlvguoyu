package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionContestantProfileMapper extends BaseMapper<CompetitionContestantProfileEntity> {

  @Select("""
      SELECT p.*
      FROM competition_contestant_profile p
      WHERE p.tenant_id = #{tenantId} AND p.member_id = #{contestantMemberId}
        AND p.deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CompetitionContestantProfileEntity selectForUpdateByTenantAndMember(
      @Param("tenantId") Long tenantId,
      @Param("contestantMemberId") Long contestantMemberId);

  @Select("""
      SELECT p.*
      FROM competition_contestant_profile p
      WHERE p.tenant_id = #{tenantId} AND p.member_id = #{contestantMemberId}
        AND p.event_work_type_id = #{eventWorkTypeId} AND p.deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CompetitionContestantProfileEntity selectForUpdateByTenantMemberAndWorkType(
      @Param("tenantId") Long tenantId,
      @Param("contestantMemberId") Long contestantMemberId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  @Select("""
      SELECT p.*
      FROM competition_contestant_profile p
      WHERE p.tenant_id = #{tenantId} AND p.registration_id = #{registrationId}
        AND p.deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CompetitionContestantProfileEntity selectForUpdateByRegistrationId(
      @Param("tenantId") Long tenantId,
      @Param("registrationId") Long registrationId);

  @Select({
      "<script>",
      "SELECT p.id AS profile_id, p.member_id AS contestant_member_id,",
      "p.registration_id, p.event_work_type_id, p.work_type_match_status,",
      "p.work_type_match_note, i.work_type AS roster_work_type",
      "FROM competition_contestant_profile p",
      "JOIN competition_event_member m ON m.id = p.member_id",
      "AND m.tenant_id = p.tenant_id AND m.deleted = 0",
      "LEFT JOIN competition_registration r ON r.id = p.registration_id",
      "AND r.tenant_id = p.tenant_id AND r.deleted = 0",
      "LEFT JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "WHERE p.tenant_id = #{tenantId} AND m.event_id = #{eventId}",
      "AND m.role_type = 'contestant' AND p.deleted = 0",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "ORDER BY p.created_at ASC, p.id ASC",
      "</script>"
  })
  List<CompetitionContestantWorkTypeRow> selectWorkTypeRowsByEvent(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId);

  /**
   * Resolves contestant profiles for a logged-in player identity against one formal exam session
   * scope. Multiple rows are possible across events; callers must refuse if more than one remains
   * after CandidateExam narrowing.
   */
  @Select("""
      SELECT p.*
      FROM competition_contestant_profile p
      JOIN competition_event_member m
        ON m.id = p.member_id AND m.tenant_id = p.tenant_id
       AND m.event_id = #{eventId} AND m.role_type = 'contestant'
       AND m.status = 'active' AND m.deleted = 0
      JOIN competition_registration r
        ON r.id = p.registration_id AND r.tenant_id = p.tenant_id
       AND r.event_id = #{eventId}
       AND r.player_id = #{playerId}
       AND r.deleted = 0
      LEFT JOIN competition_roster_item i
        ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0
      LEFT JOIN competition_event_work_type scope_wt
        ON scope_wt.id = #{eventWorkTypeId}
       AND scope_wt.tenant_id = p.tenant_id AND scope_wt.event_id = #{eventId} AND scope_wt.deleted = 0
      WHERE p.tenant_id = #{tenantId}
        AND p.deleted = 0
        AND (p.current_stage_id IS NULL OR p.current_stage_id = #{stageId})
        AND (
          p.event_work_type_id = #{eventWorkTypeId}
          OR p.event_work_type_id IS NULL
          OR (scope_wt.display_name IS NOT NULL AND i.work_type = scope_wt.display_name)
        )
        AND (
          p.event_work_type_level_id = #{eventWorkTypeLevelId}
          OR p.event_work_type_level_id IS NULL
        )
      ORDER BY p.id ASC
      """)
  List<CompetitionContestantProfileEntity> selectByPlayerAndFormalSessionScope(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId);
}
