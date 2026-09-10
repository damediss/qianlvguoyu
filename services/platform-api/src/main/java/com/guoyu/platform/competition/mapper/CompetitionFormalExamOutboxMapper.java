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

  @Select("""
      SELECT COALESCE(MAX(seq), 0) FROM competition_formal_exam_outbox
      WHERE exam_session_id = #{examSessionId}
        AND deleted = 0
      """)
  Integer selectMaxSeq(@Param("examSessionId") Long examSessionId);

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
      ORDER BY id ASC
      LIMIT #{limit}
      """)
  List<CompetitionFormalExamOutboxEntity> selectPending(@Param("limit") int limit);

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
}
