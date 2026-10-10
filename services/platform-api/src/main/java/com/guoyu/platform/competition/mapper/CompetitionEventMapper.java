package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionEventMapper extends BaseMapper<CompetitionEventEntity> {

  @Select("SELECT * FROM competition_event "
      + "WHERE tenant_id = #{tenantId} AND id = #{eventId} AND deleted = 0 FOR UPDATE")
  CompetitionEventEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("eventId") Long eventId);

  @InterceptorIgnore(tenantLine = "true")
  @Select("SELECT * FROM competition_event WHERE player_entry_code = #{entryCode} AND deleted = 0 LIMIT 1")
  CompetitionEventEntity selectByPlayerEntryCode(@Param("entryCode") String entryCode);

  @InterceptorIgnore(tenantLine = "true")
  @Select("SELECT COUNT(1) > 0 FROM competition_event WHERE player_entry_code = #{entryCode} AND deleted = 0")
  boolean existsByPlayerEntryCode(@Param("entryCode") String entryCode);
}
