package com.guoyu.platform.competition.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionSubjectScoreContestantMapper {

  @Select({
      "<script>",
      "SELECT COALESCE(p.member_id, r.member_id) AS contestant_member_id,",
      "pi.id_card_lookup_hash",
      "FROM competition_registration r",
      "JOIN competition_player_identity pi ON pi.id = r.player_id",
      "AND pi.tenant_id = r.tenant_id AND pi.deleted = 0",
      "LEFT JOIN competition_contestant_profile p ON p.registration_id = r.id",
      "AND p.tenant_id = r.tenant_id AND p.deleted = 0",
      "LEFT JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId}",
      "AND r.deleted = 0 AND r.player_id IS NOT NULL",
      "AND r.status IN ('eligible_activated', 'eligible_unactivated')",
      "AND COALESCE(p.member_id, r.member_id) IS NOT NULL",
      "AND (p.current_stage_id IS NULL OR p.current_stage_id = #{stageId})",
      "AND COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) = #{eventWorkTypeId}",
      "<if test='eventWorkTypeLevelId != null'>",
      "AND (p.event_work_type_level_id = #{eventWorkTypeLevelId} OR p.event_work_type_level_id IS NULL)",
      "</if>",
      "</script>"
  })
  List<CompetitionSubjectScoreContestantRow> selectByScope(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId);
}
