package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CertificationAssessorTaskMapper extends BaseMapper<CertificationAssessorTaskEntity> {

  @Select("""
      SELECT * FROM certification_assessor_task
      WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CertificationAssessorTaskEntity lockById(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
