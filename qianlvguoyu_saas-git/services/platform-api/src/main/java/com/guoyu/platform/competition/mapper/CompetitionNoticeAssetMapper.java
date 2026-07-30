package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionNoticeAssetEntity;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionNoticeAssetMapper extends BaseMapper<CompetitionNoticeAssetEntity> {

  @Select("""
      <script>
      SELECT a.notice_id, a.id AS asset_id, a.asset_type,
             f.original_name, f.content_type, f.size_bytes
      FROM competition_notice_asset a
      JOIN sys_file f ON f.id = a.file_id AND f.tenant_id = a.tenant_id
        AND f.status = 'uploaded' AND f.deleted = 0
      WHERE a.tenant_id = #{tenantId} AND a.deleted = 0
        AND a.notice_id IN
        <foreach collection="noticeIds" item="noticeId" open="(" separator="," close=")">
          #{noticeId}
        </foreach>
      ORDER BY a.notice_id, a.sort_order, a.id
      </script>
      """)
  List<CompetitionNoticeAssetRow> selectRowsByNoticeIds(
      @Param("tenantId") Long tenantId,
      @Param("noticeIds") List<Long> noticeIds);

  @Select("""
      SELECT a.*
      FROM competition_notice_asset a
      JOIN competition_player_message m ON m.notice_id = a.notice_id
        AND m.tenant_id = a.tenant_id AND m.deleted = 0
      JOIN competition_notice n ON n.id = m.notice_id
        AND n.tenant_id = m.tenant_id AND n.status = 'published' AND n.deleted = 0
      JOIN competition_registration r ON r.event_id = m.event_id AND r.player_id = m.player_id
        AND r.tenant_id = m.tenant_id AND r.status = 'eligible_activated' AND r.deleted = 0
      WHERE a.tenant_id = #{tenantId} AND a.id = #{assetId} AND a.deleted = 0
        AND m.id = #{messageId} AND m.player_id = #{playerId}
      LIMIT 1
      """)
  CompetitionNoticeAssetEntity selectPlayerAccessible(
      @Param("tenantId") Long tenantId,
      @Param("playerId") Long playerId,
      @Param("messageId") Long messageId,
      @Param("assetId") Long assetId);
}
