package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionTheoryPaperSnapshotMapper
    extends BaseMapper<CompetitionTheoryPaperSnapshotEntity> {

  @Select("SELECT MAX(version_no) FROM competition_theory_paper_snapshot "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND stage_id = #{stageId} AND event_work_type_id = #{eventWorkTypeId} "
      + "AND score_rule_id = #{scoreRuleId} AND paper_code = #{paperCode} AND deleted = 0")
  Integer selectMaxVersion(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("scoreRuleId") Long scoreRuleId,
      @Param("paperCode") String paperCode);

  @Update("UPDATE competition_theory_paper_snapshot SET status = 'deprecated', "
      + "updated_by = #{userId}, updated_at = NOW() "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND stage_id = #{stageId} AND event_work_type_id = #{eventWorkTypeId} "
      + "AND score_rule_id = #{scoreRuleId} AND paper_code = #{paperCode} "
      + "AND status = 'published' AND deleted = 0")
  int deprecatePublished(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("scoreRuleId") Long scoreRuleId,
      @Param("paperCode") String paperCode,
      @Param("userId") Long userId);
}
