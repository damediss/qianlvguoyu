package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamRoomEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryExamRoomMapper
    extends BaseMapper<CompetitionTheoryExamRoomEntity> {

  @Select("""
      SELECT COUNT(1)
      FROM competition_theory_session_room sr
      JOIN competition_theory_schedule_version v
        ON v.id = sr.schedule_version_id AND v.tenant_id = sr.tenant_id
        AND v.status = 'published' AND v.deleted = 0
      WHERE sr.tenant_id = #{tenantId} AND sr.room_id = #{roomId} AND sr.deleted = 0
      """)
  long countPublishedReferences(
      @Param("tenantId") Long tenantId, @Param("roomId") Long roomId);
}
