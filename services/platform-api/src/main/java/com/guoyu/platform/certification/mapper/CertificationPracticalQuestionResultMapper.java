package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationPracticalQuestionResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CertificationPracticalQuestionResultMapper
    extends BaseMapper<CertificationPracticalQuestionResultEntity> {

  @Select("""
      SELECT * FROM certification_practical_question_result
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND paper_question_id = #{questionId}
        AND deleted = 0
      LIMIT 1
      FOR UPDATE
      """)
  CertificationPracticalQuestionResultEntity lockResult(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId,
      @Param("questionId") Long questionId
  );
}
