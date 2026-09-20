package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionRefereeProfileMapper extends BaseMapper<CompetitionRefereeProfileEntity> {

  String GLOBAL_QUERY_SQL = """
      <script>
      SELECT p.id AS profile_id,
             NULL AS event_id,
             NULL AS member_id,
             p.person_id,
             p.user_id,
             p.account_username,
             NULL AS event_name,
             NULL AS event_work_type_id,
             NULL AS event_work_type_name,
             NULL AS event_work_type_level_id,
             NULL AS event_work_type_level_code,
             NULL AS event_work_type_level_name,
             p.referee_name AS name,
             i.id_card_ciphertext AS id_card_no,
             i.mobile_ciphertext AS mobile,
             p.status,
             p.created_at
        FROM competition_referee_profile p
        JOIN competition_player_identity i
          ON i.id = p.person_id
         AND i.tenant_id = p.tenant_id
         AND i.deleted = 0
       WHERE p.tenant_id = #{tenantId}
         AND p.deleted = 0
      <if test="profileId != null"> AND p.id = #{profileId}</if>
      <if test="eventId != null">
        AND EXISTS (
          SELECT 1
            FROM competition_event_member em_filter
           WHERE em_filter.tenant_id = p.tenant_id
             AND em_filter.person_id = p.person_id
             AND em_filter.event_id = #{eventId}
             AND em_filter.role_type = 'referee'
             AND em_filter.deleted = 0
        )
      </if>
      <if test="workTypeId != null">
        AND EXISTS (
          SELECT 1
            FROM competition_referee_qualification q_filter
           WHERE q_filter.tenant_id = p.tenant_id
             AND q_filter.referee_profile_id = p.id
             AND q_filter.work_type_id = #{workTypeId}
             AND q_filter.deleted = 0
        )
      </if>
      <if test="status != null and status != ''"> AND p.status = #{status}</if>
      <if test="keyword != null and keyword != ''">
        AND (p.referee_name LIKE CONCAT('%', #{keyword}, '%')
          OR i.id_card_ciphertext LIKE CONCAT('%', #{keyword}, '%')
          OR i.mobile_ciphertext LIKE CONCAT('%', #{keyword}, '%'))
      </if>
       ORDER BY p.created_at DESC, p.id DESC
      </script>
      """;

  String STAGE_QUERY_SQL = """
      <script>
      SELECT p.id AS profile_id,
             #{eventId} AS event_id,
             em.id AS member_id,
             p.person_id,
             p.user_id,
             p.account_username,
             e.name AS event_name,
             ewt.id AS event_work_type_id,
             ewt.display_name AS event_work_type_name,
             lvl.id AS event_work_type_level_id,
             #{levelCode} AS event_work_type_level_code,
             lvl.level_name AS event_work_type_level_name,
             p.referee_name AS name,
             i.id_card_ciphertext AS id_card_no,
             i.mobile_ciphertext AS mobile,
             p.status,
             p.created_at
        FROM competition_referee_profile p
        JOIN competition_player_identity i
          ON i.id = p.person_id
         AND i.tenant_id = p.tenant_id
         AND i.deleted = 0
        JOIN competition_event_work_type ewt
          ON ewt.id = #{eventWorkTypeId}
         AND ewt.tenant_id = p.tenant_id
         AND ewt.event_id = #{eventId}
         AND ewt.deleted = 0
        JOIN competition_referee_qualification q
          ON q.tenant_id = p.tenant_id
         AND q.referee_profile_id = p.id
         AND q.work_type_id = ewt.work_type_id
          AND CAST(#{levelCode} AS UNSIGNED) BETWEEN
              LEAST(
                CAST(COALESCE(q.level_from_code, q.level_code) AS UNSIGNED),
                CAST(COALESCE(q.level_to_code, q.level_code) AS UNSIGNED)
              )
              AND GREATEST(
                CAST(COALESCE(q.level_from_code, q.level_code) AS UNSIGNED),
                CAST(COALESCE(q.level_to_code, q.level_code) AS UNSIGNED)
              )
         AND q.deleted = 0
        LEFT JOIN competition_event_work_type_level lvl
          ON lvl.tenant_id = p.tenant_id
         AND lvl.event_id = #{eventId}
         AND lvl.event_work_type_id = ewt.id
          AND lvl.level_code = #{levelCode}
         AND lvl.deleted = 0
        JOIN competition_event e
          ON e.id = #{eventId}
         AND e.tenant_id = p.tenant_id
         AND e.deleted = 0
        LEFT JOIN competition_event_member em
          ON em.tenant_id = p.tenant_id
         AND em.event_id = #{eventId}
         AND em.person_id = p.person_id
         AND em.role_type = 'referee'
         AND em.deleted = 0
       WHERE p.tenant_id = #{tenantId}
         AND p.deleted = 0
         AND p.status = 'active'
       ORDER BY p.referee_name ASC, p.id ASC
      </script>
      """;

  String QUALIFICATION_QUERY_SQL = """
      <script>
      SELECT q.referee_profile_id AS profile_id,
             q.work_type_id,
             wt.title AS work_type_name,
             q.level_from_code,
             q.level_to_code,
             q.level_code
        FROM competition_referee_qualification q
        JOIN exam_work_type wt
          ON wt.id = q.work_type_id
         AND wt.tenant_id = q.tenant_id
         AND wt.deleted = 0
       WHERE q.tenant_id = #{tenantId}
         AND q.deleted = 0
      <if test="profileIds != null and profileIds.size() > 0">
         AND q.referee_profile_id IN
         <foreach collection="profileIds" item="profileId" open="(" separator="," close=")">
           #{profileId}
         </foreach>
      </if>
       ORDER BY q.referee_profile_id ASC, wt.title ASC,
                COALESCE(q.level_from_code, q.level_code) ASC,
                COALESCE(q.level_to_code, q.level_code) ASC
      </script>
      """;

  @Select({GLOBAL_QUERY_SQL})
  Page<CompetitionRefereeQueryRow> selectPage(
      Page<CompetitionRefereeQueryRow> page,
      @Param("tenantId") Long tenantId,
      @Param("profileId") Long profileId,
      @Param("eventId") Long eventId,
      @Param("workTypeId") Long workTypeId,
      @Param("status") String status,
      @Param("keyword") String keyword);

  @Select({GLOBAL_QUERY_SQL})
  List<CompetitionRefereeQueryRow> selectRows(
      @Param("tenantId") Long tenantId,
      @Param("profileId") Long profileId,
      @Param("eventId") Long eventId,
      @Param("workTypeId") Long workTypeId,
      @Param("status") String status,
      @Param("keyword") String keyword);

  @Select({STAGE_QUERY_SQL})
  List<CompetitionRefereeQueryRow> selectQualifiedForStage(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("levelCode") String levelCode);

  @Select({QUALIFICATION_QUERY_SQL})
  List<CompetitionRefereeQualificationRow> selectQualifications(
      @Param("tenantId") Long tenantId,
      @Param("profileIds") List<Long> profileIds);
}
