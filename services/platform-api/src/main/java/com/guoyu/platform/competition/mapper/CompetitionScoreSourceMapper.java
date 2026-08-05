package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionScoreSourceMapper extends BaseMapper<CompetitionScoreSourceEntity> {

  @Select("""
      SELECT COUNT(1) FROM competition_score_source
      WHERE tenant_id = #{tenantId} AND event_id = #{eventId}
        AND event_work_type_id = #{eventWorkTypeId}
        AND contestant_member_id = #{contestantMemberId}
        AND deleted = 0
      """)
  long countScopeChangeBlockers(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("contestantMemberId") Long contestantMemberId);
}
