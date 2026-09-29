package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionMakeExamPlanMapper extends BaseMapper<CompetitionMakeExamPlanEntity> {

  @Select("SELECT COALESCE(MAX(plan_version), 0) FROM competition_make_exam_plan "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND event_work_type_id = #{eventWorkTypeId} "
      + "AND event_work_type_level_id = #{eventWorkTypeLevelId} "
      + "AND content_type = #{contentType} AND plan_purpose = 'EXAM_PLAN' AND deleted = 0")
  Integer selectMaxVersion(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("contentType") String contentType);

  @Select("SELECT COALESCE(MAX(plan_version), 0) FROM competition_make_exam_plan "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND event_work_type_id = #{eventWorkTypeId} "
      + "AND event_work_type_level_id = #{eventWorkTypeLevelId} "
      + "AND content_type = #{contentType} AND plan_purpose = #{planPurpose} AND deleted = 0")
  Integer selectMaxVersionByPurpose(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("contentType") String contentType,
      @Param("planPurpose") String planPurpose);
}
