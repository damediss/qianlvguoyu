package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionStagePaperAssignmentMapper
    extends BaseMapper<CompetitionStagePaperAssignmentEntity> {

  String ELIGIBLE_PLAYERS_SQL = """
      <script>
      SELECT p.id AS contestant_profile_id, p.member_id AS contestant_member_id,
             p.registration_id, r.player_id, p.contestant_no,
             r.org_id, o.name AS org_name,
             i.name AS contestant_name, i.work_type AS roster_work_type,
             i.level AS roster_level, wt.display_name AS work_type_name,
             lvl.id AS event_work_type_level_id, lvl.level_code, lvl.level_name,
             a.paper_snapshot_id AS assigned_paper_snapshot_id,
             snap.id AS paper_snapshot_id, snap.paper_code,
             snap.source_title AS paper_title, a.allocation_source,
             a.status AS assignment_status
      FROM competition_contestant_profile p
      JOIN competition_event_member m
        ON m.id = p.member_id AND m.tenant_id = p.tenant_id
       AND m.event_id = #{eventId} AND m.role_type = 'contestant'
       AND m.status = 'active' AND m.deleted = 0
      JOIN competition_registration r
        ON r.id = p.registration_id AND r.tenant_id = p.tenant_id
       AND r.event_id = #{eventId}
       AND r.status IN ('eligible_unactivated', 'eligible_activated', 'imported_pending_confirm', 'pending_audit')
       AND r.deleted = 0
      JOIN competition_roster_item i
        ON i.id = r.roster_item_id AND i.tenant_id = r.tenant_id AND i.deleted = 0
      LEFT JOIN sys_org o
        ON o.id = r.org_id AND o.tenant_id = r.tenant_id AND o.deleted = 0
      LEFT JOIN competition_event_work_type wt
        ON wt.id = p.event_work_type_id AND wt.tenant_id = p.tenant_id AND wt.deleted = 0
      LEFT JOIN competition_event_work_type scope_wt
        ON scope_wt.id = #{eventWorkTypeId}
       AND scope_wt.tenant_id = p.tenant_id AND scope_wt.event_id = #{eventId} AND scope_wt.deleted = 0
      JOIN competition_event_work_type_level lvl
        ON lvl.id = #{eventWorkTypeLevelId}
       AND lvl.tenant_id = p.tenant_id AND lvl.event_id = #{eventId}
       AND lvl.event_work_type_id = #{eventWorkTypeId} AND lvl.deleted = 0
      LEFT JOIN competition_stage_paper_assignment a
        ON a.tenant_id = p.tenant_id AND a.event_id = #{eventId}
       AND a.stage_id = #{stageId} AND a.event_work_type_id = #{eventWorkTypeId}
       AND a.event_work_type_level_id = #{eventWorkTypeLevelId}
       AND a.contestant_profile_id = p.id AND a.deleted = 0
      LEFT JOIN competition_theory_paper_snapshot snap
        ON snap.id = a.paper_snapshot_id AND snap.tenant_id = a.tenant_id
       AND snap.event_id = #{eventId} AND snap.stage_id = #{stageId}
       AND snap.event_work_type_id = #{eventWorkTypeId}
       AND snap.event_work_type_level_id = #{eventWorkTypeLevelId}
       AND snap.status = 'published' AND snap.deleted = 0
      WHERE p.tenant_id = #{tenantId}
        AND p.deleted = 0
        AND (p.current_stage_id IS NULL OR p.current_stage_id = #{stageId})
        AND (
          p.event_work_type_id = #{eventWorkTypeId}
          OR p.event_work_type_id IS NULL
          OR (scope_wt.display_name IS NOT NULL AND i.work_type = scope_wt.display_name)
        )
        AND (p.work_type_match_status IS NULL
          OR p.work_type_match_status IN ('matched', 'unmatched', 'conflict'))
        AND (
          p.event_work_type_level_id = #{eventWorkTypeLevelId}
          OR p.event_work_type_level_id IS NULL
          <if test="levelTexts != null and levelTexts.size() > 0">
            OR i.level IN
            <foreach collection="levelTexts" item="levelText" open="(" separator="," close=")">
              #{levelText}
            </foreach>
          </if>
        )
      <if test="keyword != null and keyword != ''">
        AND (i.name LIKE CONCAT('%', #{keyword}, '%')
          OR p.contestant_no LIKE CONCAT('%', #{keyword}, '%'))
      </if>
      <if test="orgId != null">AND r.org_id = #{orgId}</if>
      <if test="paperSnapshotId != null">AND snap.id = #{paperSnapshotId}</if>
      <if test="assignmentStatus == 'assigned'">AND snap.id IS NOT NULL</if>
      <if test="assignmentStatus == 'unassigned'">AND snap.id IS NULL</if>
      ORDER BY p.id ASC
      </script>
      """;

  @Select({ELIGIBLE_PLAYERS_SQL})
  List<CompetitionStagePaperAssignmentPlayerRow> selectEligiblePlayers(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("levelTexts") List<String> levelTexts,
      @Param("keyword") String keyword,
      @Param("orgId") Long orgId,
      @Param("paperSnapshotId") Long paperSnapshotId,
      @Param("assignmentStatus") String assignmentStatus);

  @Select({ELIGIBLE_PLAYERS_SQL})
  Page<CompetitionStagePaperAssignmentPlayerRow> selectEligiblePlayersPage(
      Page<CompetitionStagePaperAssignmentPlayerRow> page,
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("levelTexts") List<String> levelTexts,
      @Param("keyword") String keyword,
      @Param("orgId") Long orgId,
      @Param("paperSnapshotId") Long paperSnapshotId,
      @Param("assignmentStatus") String assignmentStatus);

  @Select("""
      SELECT * FROM competition_stage_paper_assignment
      WHERE tenant_id = #{tenantId} AND event_id = #{eventId}
        AND stage_id = #{stageId} AND event_work_type_id = #{eventWorkTypeId}
        AND event_work_type_level_id = #{eventWorkTypeLevelId} AND deleted = 0
      ORDER BY id ASC
      """)
  List<CompetitionStagePaperAssignmentEntity> selectByScope(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId);

  @Select("""
      SELECT * FROM competition_stage_paper_assignment
      WHERE tenant_id = #{tenantId} AND event_id = #{eventId}
        AND stage_id = #{stageId} AND event_work_type_id = #{eventWorkTypeId}
        AND event_work_type_level_id = #{eventWorkTypeLevelId} AND deleted = 0
      ORDER BY id ASC
      FOR UPDATE
      """)
  List<CompetitionStagePaperAssignmentEntity> selectByScopeForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId);
}
