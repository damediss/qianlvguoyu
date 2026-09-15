package com.guoyu.platform.competition.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CompetitionOutboxEventMapper extends BaseMapper<CompetitionOutboxEventEntity> {

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT *
      FROM competition_outbox_event
      WHERE deleted = 0
        AND event_channel = #{eventChannel}
        AND (
          (status IN ('pending', 'retry') AND next_attempt_at <= #{now})
          OR (status = 'publishing' AND updated_at <= #{staleBefore})
        )
      ORDER BY created_at, id
      LIMIT #{limit}
      FOR UPDATE SKIP LOCKED
      """)
  List<CompetitionOutboxEventEntity> selectDispatchableForUpdate(
      @Param("eventChannel") String eventChannel,
      @Param("now") LocalDateTime now,
      @Param("staleBefore") LocalDateTime staleBefore,
      @Param("limit") int limit);

  @InterceptorIgnore(tenantLine = "true")
  @Update("UPDATE competition_outbox_event SET status = 'publishing', attempt_count = attempt_count + 1, "
      + "updated_at = #{updatedAt} WHERE id = #{id} AND deleted = 0")
  int markPublishing(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

  @InterceptorIgnore(tenantLine = "true")
  @Update("UPDATE competition_outbox_event SET status = 'published', published_at = #{publishedAt}, "
      + "last_error = NULL, updated_at = #{publishedAt} WHERE id = #{id} AND status = 'publishing' "
      + "AND deleted = 0")
  int markPublished(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

  @InterceptorIgnore(tenantLine = "true")
  @Update("UPDATE competition_outbox_event SET status = #{status}, next_attempt_at = #{nextAttemptAt}, "
      + "last_error = #{lastError}, updated_at = #{updatedAt} WHERE id = #{id} AND status = 'publishing' "
      + "AND deleted = 0")
  int markRetryOrFailed(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
      @Param("lastError") String lastError,
      @Param("updatedAt") LocalDateTime updatedAt);
}
