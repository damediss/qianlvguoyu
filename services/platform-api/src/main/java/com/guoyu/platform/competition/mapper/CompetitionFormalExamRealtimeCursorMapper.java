package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamRealtimeCursorEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionFormalExamRealtimeCursorMapper
    extends BaseMapper<CompetitionFormalExamRealtimeCursorEntity> {

  /**
   * Atomically allocate the next session seq on the current JDBC connection.
   * Caller must then {@link #selectLastInsertId()} on the same connection.
   */
  @InterceptorIgnore(tenantLine = "true")
  @Insert("""
      INSERT INTO competition_formal_exam_realtime_cursor
        (id, tenant_id, exam_session_id, last_seq, dispatch_seq, deleted, created_by, updated_by, created_at, updated_at)
      VALUES
        (#{id}, #{tenantId}, #{examSessionId}, LAST_INSERT_ID(1), 0, 0, #{actorId}, #{actorId}, #{now}, #{now})
      ON DUPLICATE KEY UPDATE
        last_seq = LAST_INSERT_ID(last_seq + 1),
        updated_by = #{actorId},
        updated_at = #{now}
      """)
  int allocateNextSeq(
      @Param("id") Long id,
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId,
      @Param("actorId") Long actorId,
      @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("SELECT LAST_INSERT_ID()")
  Long selectLastInsertId();

  @InterceptorIgnore(tenantLine = "true")
  @org.apache.ibatis.annotations.Update("""
      UPDATE competition_formal_exam_realtime_cursor
      SET dispatch_seq = #{seq},
          updated_at = #{now}
      WHERE tenant_id = #{tenantId}
        AND exam_session_id = #{examSessionId}
        AND deleted = 0
        AND dispatch_seq = #{previousSeq}
        AND last_seq >= #{seq}
      """)
  int advanceDispatchSeq(
      @Param("tenantId") Long tenantId,
      @Param("examSessionId") Long examSessionId,
      @Param("previousSeq") long previousSeq,
      @Param("seq") long seq,
      @Param("now") LocalDateTime now);
}
