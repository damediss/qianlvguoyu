package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateExamSessionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CertificationCandidateExamSessionMapper extends BaseMapper<CertificationCandidateExamSessionEntity> {

  @Select("""
      SELECT * FROM certification_candidate_exam_session
      WHERE tenant_id = #{tenantId} AND task_id = #{taskId}
        AND candidate_id = #{candidateId} AND exam_session_id = #{examSessionId}
      ORDER BY deleted ASC, id DESC LIMIT 1
      """)
  CertificationCandidateExamSessionEntity selectIncludingDeleted(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId,
      @Param("examSessionId") Long examSessionId);

  @Select("""
      SELECT * FROM certification_candidate_exam_session
      WHERE tenant_id = #{tenantId} AND task_id = #{taskId}
        AND candidate_id = #{candidateId} AND deleted = 0
      ORDER BY id ASC
      """)
  List<CertificationCandidateExamSessionEntity> selectByCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId);

  @Select("""
      SELECT * FROM certification_candidate_exam_session
      WHERE tenant_id = #{tenantId} AND task_id = #{taskId}
        AND exam_session_id = #{examSessionId} AND deleted = 0
      ORDER BY id ASC
      """)
  List<CertificationCandidateExamSessionEntity> selectBySession(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("examSessionId") Long examSessionId);
}
