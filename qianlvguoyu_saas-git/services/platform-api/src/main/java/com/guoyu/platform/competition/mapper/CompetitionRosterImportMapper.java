package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionRosterImportMapper extends BaseMapper<CompetitionRosterImportEntity> {

  @Select("SELECT * FROM competition_roster_import "
      + "WHERE tenant_id = #{tenantId} AND id = #{batchId} AND deleted = 0 FOR UPDATE")
  CompetitionRosterImportEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("batchId") Long batchId);
}
