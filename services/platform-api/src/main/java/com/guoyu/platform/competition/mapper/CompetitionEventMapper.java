package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionEventMapper extends BaseMapper<CompetitionEventEntity> {

  @Select("SELECT * FROM competition_event "
      + "WHERE tenant_id = #{tenantId} AND id = #{eventId} AND deleted = 0 FOR UPDATE")
  CompetitionEventEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("eventId") Long eventId);
}
