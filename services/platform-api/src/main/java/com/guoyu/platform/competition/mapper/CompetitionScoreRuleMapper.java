package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionScoreRuleMapper extends BaseMapper<CompetitionScoreRuleEntity> {

  @Select("SELECT * FROM competition_score_rule "
      + "WHERE tenant_id = #{tenantId} AND id = #{ruleId} AND deleted = 0 FOR UPDATE")
  CompetitionScoreRuleEntity selectForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("ruleId") Long ruleId);

  @Select("SELECT MAX(rule_version) FROM competition_score_rule "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND stage_id = #{stageId} "
      + "AND event_work_type_id = #{eventWorkTypeId} AND deleted = 0")
  Integer selectMaxRuleVersion(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  @Select("SELECT * FROM competition_score_rule "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND stage_id = #{stageId} "
      + "AND event_work_type_id = #{eventWorkTypeId} AND status = 'published' "
      + "AND deleted = 0 FOR UPDATE")
  List<CompetitionScoreRuleEntity> selectPublishedForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);

  @Update("UPDATE competition_score_rule SET status = 'deprecated', "
      + "updated_at = #{updatedAt}, updated_by = #{updatedBy} "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND stage_id = #{stageId} "
      + "AND event_work_type_id = #{eventWorkTypeId} AND status = 'published' "
      + "AND id <> #{currentRuleId} AND deleted = 0")
  int deprecatePublishedInScope(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("currentRuleId") Long currentRuleId,
      @Param("updatedBy") Long updatedBy,
      @Param("updatedAt") LocalDateTime updatedAt);
}
