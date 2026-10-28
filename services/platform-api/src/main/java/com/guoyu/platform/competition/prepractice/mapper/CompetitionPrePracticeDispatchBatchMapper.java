package com.guoyu.platform.competition.prepractice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeDispatchBatchEntity;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPlayerPrePracticeInboxRow;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPrePracticeContestantRow;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPrePracticeOrganizationRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompetitionPrePracticeDispatchBatchMapper
    extends BaseMapper<CompetitionPrePracticeDispatchBatchEntity> {

  @InterceptorIgnore(tenantLine = "true")
  @Select({
      "<script>",
      "SELECT p.id AS profile_id, r.id AS registration_id, r.player_id,",
      "p.contestant_no, i.name AS contestant_name, r.org_id, o.name AS org_name",
      "FROM competition_contestant_profile p",
      "JOIN competition_event_member m ON m.id = p.member_id AND m.tenant_id = p.tenant_id",
      "AND m.event_id = #{eventId} AND m.role_type = 'contestant'",
      "AND m.status = 'active' AND m.deleted = 0",
      "JOIN competition_registration r ON r.id = p.registration_id AND r.tenant_id = p.tenant_id",
      "AND r.event_id = #{eventId} AND r.status = 'eligible_activated' AND r.deleted = 0",
      "JOIN competition_player_identity pi ON pi.id = r.player_id AND pi.tenant_id = r.tenant_id",
      "AND pi.status = 'active' AND pi.deleted = 0",
      "JOIN competition_roster_item i ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0",
      "JOIN competition_event_work_type wt ON wt.id = p.event_work_type_id",
      "AND wt.tenant_id = p.tenant_id AND wt.event_id = #{eventId} AND wt.deleted = 0",
      "JOIN competition_event_work_type_level l ON l.id = p.event_work_type_level_id",
      "AND l.tenant_id = p.tenant_id AND l.event_id = #{eventId}",
      "AND l.event_work_type_id = wt.id AND l.level_code = #{levelCode} AND l.deleted = 0",
      "LEFT JOIN sys_org o ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0",
      "WHERE p.tenant_id = #{tenantId} AND r.tenant_id = #{tenantId}",
      "AND wt.id = #{eventWorkTypeId} AND p.work_type_match_status = 'matched' AND p.deleted = 0",
      "<if test='orgId != null'> AND r.org_id = #{orgId}</if>",
      "<if test='profileIds != null and profileIds.size() > 0'>",
      "AND p.id IN <foreach collection='profileIds' item='profileId' open='(' separator=',' close=')'>#{profileId}</foreach>",
      "</if>",
      "ORDER BY COALESCE(o.name, ''), i.name, p.contestant_no, p.id",
      "</script>"
  })
  Page<CompetitionPrePracticeContestantRow> pageEligibleContestants(
      Page<CompetitionPrePracticeContestantRow> page,
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("levelCode") String levelCode,
      @Param("orgId") Long orgId,
      @Param("profileIds") List<Long> profileIds);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT r.org_id AS org_id, COALESCE(o.name, '未填写单位') AS org_name
      FROM competition_contestant_profile p
      JOIN competition_event_member m ON m.id = p.member_id AND m.tenant_id = p.tenant_id
       AND m.event_id = #{eventId} AND m.role_type = 'contestant'
       AND m.status = 'active' AND m.deleted = 0
      JOIN competition_registration r ON r.id = p.registration_id AND r.tenant_id = p.tenant_id
       AND r.event_id = #{eventId} AND r.status = 'eligible_activated' AND r.deleted = 0
      JOIN competition_player_identity pi ON pi.id = r.player_id AND pi.tenant_id = r.tenant_id
       AND pi.status = 'active' AND pi.deleted = 0
      JOIN competition_event_work_type wt ON wt.id = p.event_work_type_id
       AND wt.tenant_id = p.tenant_id AND wt.event_id = #{eventId} AND wt.deleted = 0
      JOIN competition_event_work_type_level l ON l.id = p.event_work_type_level_id
       AND l.tenant_id = p.tenant_id AND l.event_id = #{eventId}
       AND l.event_work_type_id = wt.id AND l.level_code = #{levelCode} AND l.deleted = 0
      LEFT JOIN sys_org o ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0
      WHERE p.tenant_id = #{tenantId} AND r.tenant_id = #{tenantId}
        AND wt.id = #{eventWorkTypeId} AND p.work_type_match_status = 'matched' AND p.deleted = 0
      ORDER BY org_name, org_id
      """)
  List<CompetitionPrePracticeOrganizationRow> selectEligibleOrganizations(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("levelCode") String levelCode);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT r.id AS delivery_id, b.id AS batch_id, b.task_id, b.task_title_snapshot,
             b.event_id, b.event_name_snapshot, b.event_work_type_id,
             b.event_work_type_name_snapshot, b.event_work_type_level_code,
             b.event_work_type_level_name_snapshot, b.stage_id, b.stage_name_snapshot,
             t.question_count, r.status, r.delivered_at, b.pushed_at
      FROM competition_pre_practice_dispatch_recipient r
      JOIN competition_pre_practice_dispatch_batch b
        ON b.id = r.batch_id AND b.tenant_id = r.tenant_id AND b.deleted = 0
      JOIN competition_pre_practice_task t
        ON t.id = r.task_id AND t.tenant_id = r.tenant_id AND t.deleted = 0
      WHERE r.tenant_id = #{tenantId} AND r.player_id = #{playerId} AND r.deleted = 0
      ORDER BY b.pushed_at DESC, r.id DESC
      """)
  Page<CompetitionPlayerPrePracticeInboxRow> pagePlayerInbox(
      Page<CompetitionPlayerPrePracticeInboxRow> page,
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId);
}
