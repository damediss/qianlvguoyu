package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionNoticeMapper extends BaseMapper<CompetitionNoticeEntity> {

  @Select("SELECT * FROM competition_notice "
      + "WHERE tenant_id = #{tenantId} AND id = #{noticeId} AND deleted = 0 FOR UPDATE")
  CompetitionNoticeEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("noticeId") Long noticeId);
}
