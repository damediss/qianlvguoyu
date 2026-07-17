package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionEventOrgMapper extends BaseMapper<CompetitionEventOrgEntity> {

  @Select("SELECT * FROM competition_event_org "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} AND org_id = #{orgId} "
      + "AND deleted = 0 FOR UPDATE")
  CompetitionEventOrgEntity selectForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("orgId") Long orgId);
}
