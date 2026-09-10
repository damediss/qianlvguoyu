package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessCommandEntity;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalExamAccessCommandMapper
    extends BaseMapper<CompetitionFormalExamAccessCommandEntity> {

  @Update("""
      UPDATE competition_formal_exam_access_command
      SET status = 'acked',
          ack_at = #{ackAt},
          updated_at = #{ackAt}
      WHERE id = #{id}
        AND access_session_id = #{accessSessionId}
        AND generation = #{generation}
        AND status = 'issued'
        AND deleted = 0
      """)
  int markAckedIfIssued(
      @Param("id") Long id,
      @Param("accessSessionId") Long accessSessionId,
      @Param("generation") int generation,
      @Param("ackAt") java.time.LocalDateTime ackAt);
}
