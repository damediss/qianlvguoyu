package com.guoyu.platform.competition.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.messaging.entity.CompetitionPlayerMessageEntity;
import com.guoyu.platform.competition.messaging.vo.CompetitionPlayerMessageRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionPlayerMessageMapper extends BaseMapper<CompetitionPlayerMessageEntity> {

  @Insert("INSERT INTO competition_player_message "
      + "(id, tenant_id, player_id, event_id, notice_id, type, received_at, "
      + "created_by, updated_by, created_at, updated_at, deleted) "
      + "VALUES (#{id}, #{tenantId}, #{playerId}, #{eventId}, #{noticeId}, #{type}, #{receivedAt}, "
      + "#{createdBy}, #{updatedBy}, #{createdAt}, #{updatedAt}, #{deleted}) "
      + "ON DUPLICATE KEY UPDATE id = id")
  int insertIgnoreDuplicate(CompetitionPlayerMessageEntity entity);

  @Select("""
      SELECT m.id AS message_id, m.event_id, m.notice_id, m.type, m.received_at, m.read_at,
             n.title, n.content_type, n.content, n.published_at
      FROM competition_player_message m
      JOIN competition_notice n ON n.id = m.notice_id
        AND n.tenant_id = m.tenant_id AND n.status = 'published' AND n.deleted = 0
      JOIN competition_registration r ON r.event_id = m.event_id AND r.player_id = m.player_id
        AND r.tenant_id = m.tenant_id AND r.status = 'eligible_activated' AND r.deleted = 0
      WHERE m.tenant_id = #{tenantId} AND m.player_id = #{playerId} AND m.deleted = 0
      ORDER BY m.received_at DESC, m.id DESC
      """)
  Page<CompetitionPlayerMessageRow> selectPageAccessible(
      Page<CompetitionPlayerMessageRow> page,
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId);

  @Select("""
      SELECT COUNT(1)
      FROM competition_player_message m
      JOIN competition_notice n ON n.id = m.notice_id
        AND n.tenant_id = m.tenant_id AND n.status = 'published' AND n.deleted = 0
      JOIN competition_registration r ON r.event_id = m.event_id AND r.player_id = m.player_id
        AND r.tenant_id = m.tenant_id AND r.status = 'eligible_activated' AND r.deleted = 0
      WHERE m.tenant_id = #{tenantId} AND m.player_id = #{playerId}
        AND m.read_at IS NULL AND m.deleted = 0
      """)
  long countUnreadAccessible(@Param("tenantId") Long tenantId, @Param("playerId") Long playerId);

  @Update("""
      UPDATE competition_player_message m
      JOIN competition_registration r ON r.event_id = m.event_id AND r.player_id = m.player_id
        AND r.tenant_id = m.tenant_id AND r.status = 'eligible_activated' AND r.deleted = 0
      JOIN competition_notice n ON n.id = m.notice_id
        AND n.tenant_id = m.tenant_id AND n.status = 'published' AND n.deleted = 0
      SET m.read_at = #{readAt}, m.updated_at = #{readAt}
      WHERE m.tenant_id = #{tenantId} AND m.player_id = #{playerId} AND m.id = #{messageId}
        AND m.read_at IS NULL AND m.deleted = 0
      """)
  int markReadIfAccessible(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("messageId") Long messageId,
      @Param("readAt") java.time.LocalDateTime readAt);
}
