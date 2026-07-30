package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.PracticalFingerprintTraceEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PracticalFingerprintTraceMapper extends BaseMapper<PracticalFingerprintTraceEntity> {

  @Select("""
      SELECT *
      FROM certification_practical_fingerprint_trace
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND request_id = #{requestId}
        AND deleted = 0
      ORDER BY created_at DESC
      LIMIT 1
      """)
  PracticalFingerprintTraceEntity selectByRequestId(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId,
      @Param("requestId") String requestId
  );
}
