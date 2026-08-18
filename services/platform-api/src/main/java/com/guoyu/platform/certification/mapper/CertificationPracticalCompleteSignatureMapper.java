package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignatureEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CertificationPracticalCompleteSignatureMapper
    extends BaseMapper<CertificationPracticalCompleteSignatureEntity> {

  @Select("""
      SELECT * FROM certification_practical_complete_signature
      WHERE tenant_id = #{tenantId}
        AND session_id = #{sessionId}
        AND assessor_id = #{assessorId}
        AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CertificationPracticalCompleteSignatureEntity lockByAssessor(
      @Param("tenantId") Long tenantId,
      @Param("sessionId") Long sessionId,
      @Param("assessorId") Long assessorId
  );
}
