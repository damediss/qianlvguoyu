package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface CertificationCandidateDocumentMapper extends BaseMapper<CertificationCandidateDocumentEntity> {

  @Delete("""
      DELETE FROM certification_candidate_document
      WHERE task_id = #{taskId}
        AND candidate_id = #{candidateId}
      """)
  int physicalDeleteByCandidate(@Param("taskId") Long taskId, @Param("candidateId") Long candidateId);
}
