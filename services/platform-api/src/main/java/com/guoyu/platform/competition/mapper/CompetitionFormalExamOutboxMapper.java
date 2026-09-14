package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalExamOutboxMapper
    extends BaseMapper<CompetitionFormalExamOutboxEntity> {

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT * FROM competition_formal_exam_outbox
      WHERE id = #{id}
        AND deleted = 0
      """)
  CompetitionFormalExamOutboxEntity selectByIdIgnoreTenant(@Param("id") Long id);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT * FROM competition_formal_exam_outbox
      WHERE status = 'pending'
        AND deleted = 0
      ORDER BY exam_session_id ASC, seq ASC, id ASC
      LIMIT #{limit}
      """)
  List<CompetitionFormalExamOutboxEntity> selectPending(@Param("limit") int limit);

  /**
   * Select the exact next pending event for each session while holding a short
   * database row lock. dispatch_seq is independent from the allocation
   * high-watermark last_seq, so this is an indexed point lookup instead of a
   * correlated scan over already-published history. SKIP LOCKED lets two API
   * nodes divide heads without loading the same row.
   */
  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT o.*
      FROM competition_formal_exam_outbox o
      LEFT JOIN competition_formal_exam_realtime_dispatch_cursor c
        ON c.tenant_id = o.tenant_id
       AND c.exam_session_id = o.exam_session_id
       AND c.deleted = 0
      WHERE o.status = 'pending'
        AND o.deleted = 0
        AND (
          (c.id IS NULL AND o.seq = 1)
          OR (c.id IS NOT NULL AND o.seq = c.dispatch_seq + 1)
        )
      ORDER BY o.exam_session_id ASC, o.seq ASC, o.id ASC
      LIMIT #{limit}
      FOR UPDATE SKIP LOCKED
      """)
  List<CompetitionFormalExamOutboxEntity> selectPendingHeadsForUpdate(@Param("limit") int limit);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT *
      FROM competition_formal_exam_outbox
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND status = 'pending'
        AND deleted = 0
        AND seq >= #{headSeq}
        AND seq < (#{headSeq} + #{limit})
      ORDER BY seq ASC, id ASC
      LIMIT #{limit}
      FOR UPDATE
      """)
  List<CompetitionFormalExamOutboxEntity> selectPendingBatchForSessionForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId,
      @Param("headSeq") long headSeq,
      @Param("limit") int limit);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT o.*
      FROM competition_formal_exam_outbox o
      LEFT JOIN competition_formal_exam_realtime_dispatch_cursor c
        ON c.tenant_id = o.tenant_id
       AND c.exam_session_id = o.exam_session_id
       AND c.deleted = 0
      WHERE o.id = #{id}
        AND o.status = 'pending'
        AND o.deleted = 0
        AND (
          (c.id IS NULL AND o.seq = 1)
          OR (c.id IS NOT NULL AND o.seq = c.dispatch_seq + 1)
        )
      FOR UPDATE SKIP LOCKED
      """)
  CompetitionFormalExamOutboxEntity selectPendingHeadByIdForUpdate(@Param("id") Long id);

  /** The row has already been locked by one of the head-select queries. */
  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      UPDATE competition_formal_exam_outbox
      SET status = 'publishing',
          attempts = attempts + 1,
          updated_at = #{now}
      WHERE id = #{id}
        AND status = 'pending'
        AND deleted = 0
      """)
  int markPublishing(@Param("id") Long id, @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      <script>
      UPDATE competition_formal_exam_outbox
      SET status = 'publishing',
          attempts = attempts + 1,
          updated_at = #{now}
      WHERE status = 'pending'
        AND deleted = 0
        AND id IN
      <foreach collection='ids' item='id' open='(' separator=',' close=')'>
        #{id}
      </foreach>
      </script>
      """)
  int markPublishingBatch(
      @Param("ids") List<Long> ids,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT id
      FROM competition_formal_exam_outbox
      WHERE status = 'publishing'
        AND deleted = 0
        AND updated_at <= #{expiredBefore}
      ORDER BY updated_at ASC, id ASC
      LIMIT #{limit}
      FOR UPDATE SKIP LOCKED
      """)
  List<Long> selectExpiredPublishingForUpdate(
      @Param("expiredBefore") LocalDateTime expiredBefore,
      @Param("limit") int limit);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      UPDATE competition_formal_exam_outbox
      SET status = 'pending',
          last_error = 'publishing-lease-expired',
          updated_at = #{now}
      WHERE id = #{id}
        AND status = 'publishing'
        AND deleted = 0
      """)
  int resetExpiredPublishing(@Param("id") Long id, @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      <script>
      UPDATE competition_formal_exam_outbox
      SET status = 'pending',
          last_error = 'publishing-lease-expired',
          updated_at = #{now}
      WHERE status = 'publishing'
        AND deleted = 0
        AND id IN
      <foreach collection='ids' item='id' open='(' separator=',' close=')'>
        #{id}
      </foreach>
      </script>
      """)
  int resetExpiredPublishingBatch(
      @Param("ids") List<Long> ids,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      UPDATE competition_formal_exam_outbox
      SET status = #{status},
          published_at = #{publishedAt},
          last_error = #{lastError},
          updated_at = #{publishedAt}
      WHERE id = #{id}
        AND status = 'publishing'
        AND deleted = 0
      """)
  int markFinished(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("publishedAt") LocalDateTime publishedAt,
      @Param("lastError") String lastError);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      <script>
      UPDATE competition_formal_exam_outbox
      SET status = 'published',
          published_at = #{publishedAt},
          last_error = NULL,
          updated_at = #{publishedAt}
      WHERE status = 'publishing'
        AND deleted = 0
        AND id IN
      <foreach collection='ids' item='id' open='(' separator=',' close=')'>
        #{id}
      </foreach>
      </script>
      """)
  int markPublishedBatch(
      @Param("ids") List<Long> ids,
      @Param("publishedAt") LocalDateTime publishedAt);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      UPDATE competition_formal_exam_outbox
      SET status = 'pending',
          last_error = #{lastError},
          updated_at = #{now}
      WHERE id = #{id}
        AND status = 'publishing'
        AND deleted = 0
      """)
  int markPendingRetry(
      @Param("id") Long id,
      @Param("lastError") String lastError,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Update("""
      <script>
      UPDATE competition_formal_exam_outbox
      SET status = 'pending',
          last_error = #{lastError},
          updated_at = #{now}
      WHERE status = 'publishing'
        AND deleted = 0
        AND id IN
      <foreach collection='ids' item='id' open='(' separator=',' close=')'>
        #{id}
      </foreach>
      </script>
      """)
  int markPendingRetryBatch(
      @Param("ids") List<Long> ids,
      @Param("lastError") String lastError,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT COUNT(*) FROM competition_formal_exam_outbox
      WHERE deleted = 0 AND status = #{status}
      """)
  long countByStatus(@Param("status") String status);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT TIMESTAMPDIFF(SECOND, MIN(created_at), NOW())
      FROM competition_formal_exam_outbox
      WHERE deleted = 0
        AND status IN ('pending', 'publishing')
      """)
  Long oldestPendingAgeSeconds();

  @Select("""
      SELECT COALESCE(MAX(seq), 0) FROM competition_formal_exam_outbox
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND deleted = 0
      """)
  Long selectHighWatermark(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT COALESCE(MIN(seq), 0) FROM competition_formal_exam_outbox
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND deleted = 0
      """)
  Long selectMinSeq(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT * FROM competition_formal_exam_outbox
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND deleted = 0
        AND seq > #{afterSeq}
        AND seq <= #{highWatermark}
      ORDER BY seq ASC, id ASC
      LIMIT #{limit}
      """)
  List<CompetitionFormalExamOutboxEntity> selectCatchUp(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId,
      @Param("afterSeq") long afterSeq,
      @Param("highWatermark") long highWatermark,
      @Param("limit") int limit);
}
