package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PracticalCollectionStatusMapper extends BaseMapper<PracticalCollectionStatusEntity> {

  @Select("""
      SELECT *
      FROM certification_practical_collection_status
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND ((#{examSessionId} IS NULL AND exam_session_id IS NULL) OR exam_session_id = #{examSessionId})
        AND candidate_id = #{candidateId}
        AND deleted = 0
      LIMIT 1
      """)
  PracticalCollectionStatusEntity selectByTaskAndCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("examSessionId") Long examSessionId,
      @Param("candidateId") Long candidateId
  );

  @Select("""
      SELECT *
      FROM certification_practical_collection_status
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND ((#{examSessionId} IS NULL AND exam_session_id IS NULL) OR exam_session_id = #{examSessionId})
        AND candidate_id = #{candidateId}
        AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  PracticalCollectionStatusEntity lockByTaskAndCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("examSessionId") Long examSessionId,
      @Param("candidateId") Long candidateId
  );

  @Select("""
      SELECT *
      FROM certification_practical_collection_status
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND ((#{examSessionId} IS NULL AND exam_session_id IS NULL) OR exam_session_id = #{examSessionId})
        AND collect_token = #{collectToken}
        AND deleted = 0
      LIMIT 1 FOR UPDATE
      """)
  PracticalCollectionStatusEntity lockByTaskAndToken(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("examSessionId") Long examSessionId,
      @Param("collectToken") String collectToken
  );
}
