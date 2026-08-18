package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CertificationPracticalCompleteSignSessionMapper
    extends BaseMapper<CertificationPracticalCompleteSignSessionEntity> {

  @Select("""
      SELECT * FROM certification_practical_complete_sign_session
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CertificationPracticalCompleteSignSessionEntity lockByTask(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId
  );
}
