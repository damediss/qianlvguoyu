package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionContestantProfileMapper extends BaseMapper<CompetitionContestantProfileEntity> {

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
}
