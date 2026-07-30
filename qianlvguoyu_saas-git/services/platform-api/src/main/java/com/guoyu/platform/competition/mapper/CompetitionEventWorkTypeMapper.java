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
}
