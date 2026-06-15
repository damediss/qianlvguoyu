package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CertificationCandidateSourceStagingMapper extends BaseMapper<CertificationCandidateSourceStagingEntity> {

  @Update("""
      UPDATE certification_candidate_source_staging
      SET candidate_id = NULL,
          promote_status = 'draft',
          promote_message = NULL,
          promoted_at = NULL,
          updated_at = NOW()
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND deleted = 0
      """)
  int releasePromotedCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId
  );
}
