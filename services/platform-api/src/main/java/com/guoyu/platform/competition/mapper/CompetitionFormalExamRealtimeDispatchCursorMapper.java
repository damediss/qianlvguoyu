package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamRealtimeDispatchCursorEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalExamRealtimeDispatchCursorMapper
    extends BaseMapper<CompetitionFormalExamRealtimeDispatchCursorEntity> {

  /**
   * Advance an existing dispatch cursor, or create the first cursor for a
   * session. The compare-and-set keeps a duplicate/recovered delivery from
   * moving the pointer past an event that has not completed.
   */
  @InterceptorIgnore(tenantLine = "true")
  @Insert("""
      INSERT INTO competition_formal_exam_realtime_dispatch_cursor
        (id, tenant_id, exam_session_id, dispatch_seq,
         deleted, created_by, updated_by, created_at, updated_at)
      VALUES
        (#{id}, #{tenantId}, #{examSessionId}, #{seq},
         0, 0, 0, #{now}, #{now})
      ON DUPLICATE KEY UPDATE
        dispatch_seq = IF(dispatch_seq = #{previousSeq}, #{seq}, dispatch_seq),
        updated_at = IF(dispatch_seq = #{seq}, #{now}, updated_at)
      """)
  int advanceOrCreate(
      @Param("id") Long id,
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId,
      @Param("previousSeq") long previousSeq,
      @Param("seq") long seq,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT dispatch_seq
      FROM competition_formal_exam_realtime_dispatch_cursor
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND deleted = 0
      """)
  Long selectDispatchSeq(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId);
}
