package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionEventWorkTypeMapper extends BaseMapper<CompetitionEventWorkTypeEntity> {

  @Select("SELECT COUNT(1) FROM competition_score_rule "
      + "WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} "
      + "AND status = 'published' AND deleted = 0")
  long countPublishedRuleReferences(
      @Param("tenantId") Long tenantId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  /**
   * 参赛工种一旦进入赛段、报名、试题、评分或考试，就不能再当成创建期的误选项删掉。
   * 等级和工种赛段编排不在这里，移除时由领域服务单独逻辑删除。
   */
  @Select("""
      SELECT
        (SELECT COUNT(1) FROM competition_stage WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_assignment WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_execution_plan WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_paper_assignment WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_promotion_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_reference_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_roster_item WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_roster_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_settlement WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_stage_unlock_outbox WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_contestant_profile WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_registration WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_roster_import WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_roster_item WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_question WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_question_blueprint WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_question_import_batch WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_score_rule WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_score_source WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_ranking_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_total_score_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_subject_score_import_batch WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_simulation_score_import_batch WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_make_exam_plan WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_make_exam_paper_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_requirement WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_paper_snapshot WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_exam_session WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_attempt WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_candidate_assignment WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_theory_schedule_version WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_formal_exam_session WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_rule WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_session WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_score WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_score_sheet WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_assessment_sheet WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_execution_config WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_practical_paper_binding WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_referee_group WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_referee_assignment WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_referee_avoidance WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
        + (SELECT COUNT(1) FROM competition_referee_profile WHERE tenant_id = #{tenantId} AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0)
      """)
  long countOperationalReferences(
      @Param("tenantId") Long tenantId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);
}
