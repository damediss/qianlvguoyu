package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionRegistrationMapper extends BaseMapper<CompetitionRegistrationEntity> {

  @Select("SELECT * FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND id = #{registrationId} AND deleted = 0 FOR UPDATE")
  CompetitionRegistrationEntity selectForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("registrationId") Long registrationId);

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM competition_registration r",
      "LEFT JOIN competition_contestant_profile p ON p.registration_id = r.id",
      "AND p.tenant_id = r.tenant_id AND p.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId}",
      "AND r.player_id = #{playerId} AND r.deleted = 0",
      "AND r.status NOT IN ('rejected', 'withdrawn', 'expired')",
      "<if test='eventWorkTypeId != null'>",
      "AND COALESCE(r.event_work_type_id, p.event_work_type_id) = #{eventWorkTypeId}",
      "</if>",
      "</script>"
  })
  boolean existsEffectiveByPlayerAndWorkType(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("playerId") Long playerId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  @Select("SELECT COUNT(1) FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND org_id = #{orgId} "
      + "AND player_id = #{playerId} AND deleted = 0 AND status IN ("
      + "'pending_audit', 'supplement_required', 'eligible_unactivated', 'eligible_activated')")
  boolean existsEffectiveByPlayer(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId,
      @Param("playerId") Long playerId);

  @Select("SELECT COUNT(1) FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND org_id = #{orgId} "
      + "AND deleted = 0 AND status IN ("
      + "'pending_audit', 'supplement_required', 'eligible_unactivated', 'eligible_activated')")
  long countEffectiveByEventOrg(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId);

  @Select("SELECT COUNT(DISTINCT player_id) FROM competition_registration "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND org_id = #{orgId} "
      + "AND player_id IS NOT NULL AND deleted = 0 AND status IN ("
      + "'pending_audit', 'supplement_required', 'eligible_unactivated', 'eligible_activated')")
  long countDistinctEffectivePlayersByEventOrg(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId);

  @Select("SELECT DISTINCT e.id, e.name, e.start_at, e.end_at "
      + "FROM competition_registration r "
      + "JOIN competition_event e ON e.id = r.event_id AND e.tenant_id = r.tenant_id AND e.deleted = 0 "
      + "WHERE r.tenant_id = #{tenantId} AND r.player_id = #{playerId} "
      + "AND r.event_id <> #{excludedEventId} AND r.deleted = 0 "
      + "AND r.status IN ('pending_audit', 'supplement_required', "
      + "'eligible_unactivated', 'eligible_activated')")
  List<CompetitionEventEntity> selectOtherEffectiveEventsByPlayer(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("excludedEventId") Long excludedEventId);

  @Select("SELECT r.* FROM competition_registration r "
      + "JOIN competition_player_identity p ON p.id = r.player_id "
      + "AND p.tenant_id = r.tenant_id AND p.deleted = 0 "
      + "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId} "
      + "AND p.mobile_lookup_hash = #{mobileLookupHash} AND r.status = 'eligible_unactivated' "
      + "AND r.deleted = 0 LIMIT 1 FOR UPDATE")
  CompetitionRegistrationEntity selectEligibleUnactivatedForPlayerActivation(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("mobileLookupHash") String mobileLookupHash);

  @org.apache.ibatis.annotations.Update("UPDATE competition_registration "
      + "SET status = 'eligible_activated', updated_at = #{updatedAt}, updated_by = #{updatedBy} "
      + "WHERE tenant_id = #{tenantId} AND player_id = #{playerId} "
      + "AND status = 'eligible_unactivated' AND deleted = 0")
  int activateEligibleRegistrations(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("updatedBy") Long updatedBy,
      @Param("updatedAt") java.time.LocalDateTime updatedAt);

  @Select("SELECT DISTINCT r.player_id FROM competition_registration r "
      + "JOIN competition_player_identity p ON p.id = r.player_id "
      + "AND p.tenant_id = r.tenant_id AND p.deleted = 0 "
      + "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId} "
      + "AND r.status = 'eligible_activated' AND r.player_id IS NOT NULL "
      + "AND p.status = 'active' AND r.deleted = 0")
  List<Long> selectActivePlayerIdsByEvent(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId);

  @Select({
      "<script>",
      "SELECT r.id AS registration_id, r.event_id, e.name AS event_name,",
      "r.org_id, o.name AS org_name, r.roster_item_id, i.name,",
      "i.work_type, i.level, r.source, r.status, r.audit_reason, r.audited_by,",
      "r.audited_at, r.created_at",
      "FROM competition_registration r",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event e ON e.id = r.event_id",
      "AND e.tenant_id = r.tenant_id AND e.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id",
      "AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.deleted = 0",
      "<if test='eventId != null'> AND r.event_id = #{eventId}</if>",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "<if test='status != null and status != \"\"'> AND r.status = #{status}</if>",
      "<if test='name != null and name != \"\"'> AND i.name LIKE CONCAT('%', #{name}, '%')</if>",
      "ORDER BY r.created_at DESC, r.id DESC",
      "</script>"
  })
  Page<CompetitionRegistrationQueryRow> selectPageWithDetails(
      Page<CompetitionRegistrationQueryRow> page,
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId,
      @Param("status") String status,
      @Param("name") String name);

  @Select({
      "<script>",
      "SELECT r.id AS registration_id, r.event_id, e.name AS event_name,",
      "r.org_id, o.name AS org_name, r.roster_item_id, r.player_id, r.member_id,",
      "p.id AS profile_id, COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) AS event_work_type_id, wt.display_name AS event_work_type_name,",
      "l.id AS event_work_type_level_id, l.level_code AS event_work_type_level_code, l.level_name AS event_work_type_level_name,",
      "i.name, i.work_type, COALESCE(l.level_name, i.level) AS level, r.status, pi.status AS player_status,",
      "pi.id_card_ciphertext AS id_card_no, pi.mobile_ciphertext AS mobile, r.created_at",
      "FROM competition_registration r",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event e ON e.id = r.event_id",
      "AND e.tenant_id = r.tenant_id AND e.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id",
      "AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "LEFT JOIN competition_player_identity pi ON pi.id = r.player_id",
      "AND pi.tenant_id = r.tenant_id AND pi.deleted = 0",
      "LEFT JOIN competition_contestant_profile p ON p.registration_id = r.id",
      "AND p.tenant_id = r.tenant_id AND p.deleted = 0",
      "LEFT JOIN competition_event_work_type wt ON wt.id = COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id)",
      "AND wt.tenant_id = r.tenant_id AND wt.deleted = 0",
      "LEFT JOIN competition_event_work_type_level l ON l.id = p.event_work_type_level_id",
      "AND l.tenant_id = r.tenant_id AND l.event_id = r.event_id AND l.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.deleted = 0 AND r.player_id IS NOT NULL",
      "<if test='eventId != null'> AND r.event_id = #{eventId}</if>",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "<if test='eventWorkTypeId != null'> AND COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) = #{eventWorkTypeId}</if>",
      "<if test='keyword != null and keyword != \"\"'> AND i.name LIKE CONCAT('%', #{keyword}, '%')</if>",
      "<if test='name != null and name != \"\"'> AND i.name LIKE CONCAT('%', #{name}, '%')</if>",
      "<if test='status != null and status != \"\"'> AND r.status = #{status}</if>",
      "ORDER BY r.event_id ASC, r.created_at DESC, r.id DESC",
      "</script>"
  })
  Page<CompetitionPlayerQueryRow> selectPlayerPage(
      Page<CompetitionPlayerQueryRow> page,
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("keyword") String keyword,
      @Param("name") String name,
      @Param("status") String status);

  @Select({
      "SELECT r.id AS registration_id, r.event_id, e.name AS event_name,",
      "r.org_id, o.name AS org_name, r.roster_item_id, r.player_id, r.member_id,",
      "p.id AS profile_id, COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) AS event_work_type_id, wt.display_name AS event_work_type_name,",
      "l.id AS event_work_type_level_id, l.level_code AS event_work_type_level_code, l.level_name AS event_work_type_level_name,",
      "i.name, i.work_type, COALESCE(l.level_name, i.level) AS level, r.status, pi.status AS player_status,",
      "pi.id_card_ciphertext AS id_card_no, pi.mobile_ciphertext AS mobile, r.created_at",
      "FROM competition_registration r",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event e ON e.id = r.event_id AND e.tenant_id = r.tenant_id AND e.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "LEFT JOIN competition_player_identity pi ON pi.id = r.player_id AND pi.tenant_id = r.tenant_id AND pi.deleted = 0",
      "LEFT JOIN competition_contestant_profile p ON p.registration_id = r.id AND p.tenant_id = r.tenant_id AND p.deleted = 0",
      "LEFT JOIN competition_event_work_type wt ON wt.id = COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) AND wt.tenant_id = r.tenant_id AND wt.deleted = 0",
      "LEFT JOIN competition_event_work_type_level l ON l.id = p.event_work_type_level_id AND l.tenant_id = r.tenant_id AND l.event_id = r.event_id AND l.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.id = #{registrationId} AND r.deleted = 0 LIMIT 1"
  })
  CompetitionPlayerQueryRow selectPlayerByRegistrationId(
      @Param("tenantId") Long tenantId,
      @Param("registrationId") Long registrationId);

  @Select({
      "<script>",
      "SELECT r.id AS registration_id, r.event_id, e.name AS event_name,",
      "r.org_id, o.name AS org_name, r.roster_item_id, r.player_id, r.member_id,",
      "p.id AS profile_id, COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id) AS event_work_type_id,",
      "wt.display_name AS event_work_type_name,",
      "l.id AS event_work_type_level_id, l.level_code AS event_work_type_level_code, l.level_name AS event_work_type_level_name,",
      "i.name, i.work_type, COALESCE(l.level_name, i.level) AS level,",
      "r.status, pi.status AS player_status, pi.id_card_ciphertext AS id_card_no,",
      "pi.mobile_ciphertext AS mobile, r.created_at",
      "FROM competition_registration r",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id",
      "AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event e ON e.id = r.event_id",
      "AND e.tenant_id = r.tenant_id AND e.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id",
      "AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "LEFT JOIN competition_player_identity pi ON pi.id = r.player_id",
      "AND pi.tenant_id = r.tenant_id AND pi.deleted = 0",
      "LEFT JOIN competition_contestant_profile p ON p.registration_id = r.id",
      "AND p.tenant_id = r.tenant_id AND p.deleted = 0",
      "LEFT JOIN competition_event_work_type wt ON wt.id = COALESCE(p.event_work_type_id, r.event_work_type_id, i.event_work_type_id)",
      "AND wt.tenant_id = r.tenant_id AND wt.deleted = 0",
      "LEFT JOIN competition_event_work_type_level l ON l.id = p.event_work_type_level_id",
      "AND l.tenant_id = r.tenant_id AND l.event_id = r.event_id AND l.deleted = 0",
      "WHERE r.tenant_id = #{tenantId} AND r.event_id = #{eventId}",
      "AND r.player_id = #{playerId} AND r.deleted = 0",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "ORDER BY r.created_at ASC, r.id ASC",
      "</script>"
  })
  List<CompetitionPlayerQueryRow> selectPlayerRowsByEventAndPlayer(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("playerId") Long playerId,
      @Param("orgId") Long orgId);
}
