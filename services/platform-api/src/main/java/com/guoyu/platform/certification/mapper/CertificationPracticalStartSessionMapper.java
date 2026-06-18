package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationPracticalStartSessionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CertificationPracticalStartSessionMapper
    extends BaseMapper<CertificationPracticalStartSessionEntity> {

  @Select("""
      SELECT * FROM certification_practical_start_session
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  CertificationPracticalStartSessionEntity lockSession(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId
  );

  @Select("""
      SELECT * FROM certification_practical_start_session
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND group_id = #{groupId}
        AND status = 'started'
        AND deleted = 0
      FOR UPDATE
      """)
  List<CertificationPracticalStartSessionEntity> lockStartedSessionsByGroup(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("groupId") Long groupId
  );
}
