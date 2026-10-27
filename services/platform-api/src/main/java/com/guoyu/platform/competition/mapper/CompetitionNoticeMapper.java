package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionNoticeMapper extends BaseMapper<CompetitionNoticeEntity> {

  @Select("SELECT * FROM competition_notice "
      + "WHERE tenant_id = #{tenantId} AND id = #{noticeId} AND deleted = 0 FOR UPDATE")
  CompetitionNoticeEntity selectForUpdate(
      @Param("tenantId") Long tenantId, @Param("noticeId") Long noticeId);

  @Select("""
      SELECT DISTINCT n.*
      FROM competition_notice n
      JOIN competition_registration r ON r.tenant_id = n.tenant_id
        AND r.event_id = n.event_id AND r.player_id = #{playerId}
        AND r.status = 'eligible_activated' AND r.deleted = 0
      JOIN competition_player_identity p ON p.tenant_id = n.tenant_id
        AND p.id = r.player_id AND p.status = 'active' AND p.deleted = 0
      LEFT JOIN competition_player_message m ON m.tenant_id = n.tenant_id
        AND m.notice_id = n.id AND m.player_id = #{playerId} AND m.deleted = 0
      WHERE n.tenant_id = #{tenantId} AND n.event_id = #{eventId}
        AND n.status = 'published' AND n.deleted = 0 AND m.id IS NULL
      ORDER BY n.published_at ASC, n.id ASC
      """)
  List<CompetitionNoticeEntity> selectPublishedMissingFromPlayerInbox(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("playerId") Long playerId);
}
