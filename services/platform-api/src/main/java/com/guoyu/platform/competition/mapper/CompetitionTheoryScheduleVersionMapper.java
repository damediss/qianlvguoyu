package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryScheduleVersionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryScheduleVersionMapper
    extends BaseMapper<CompetitionTheoryScheduleVersionEntity> {

  @Select("""
      SELECT * FROM competition_theory_schedule_version
      WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0
      FOR UPDATE
      """)
  CompetitionTheoryScheduleVersionEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("id") Long id);

  @Select("""
      SELECT COALESCE(MAX(version_no), 0)
      FROM competition_theory_schedule_version
      WHERE tenant_id = #{tenantId} AND event_id = #{eventId}
        AND stage_id = #{stageId} AND event_work_type_id = #{eventWorkTypeId}
        AND deleted = 0
      """)
  int selectMaxVersion(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("stageId") Long stageId,
      @Param("eventWorkTypeId") Long eventWorkTypeId);
}
