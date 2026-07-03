package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.PracticalCollectionQueueEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PracticalCollectionQueueMapper extends BaseMapper<PracticalCollectionQueueEntity> {

  @Select("""
      SELECT *
      FROM certification_practical_collection_queue
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND queue_status IN ('QUEUED', 'COLLECTING')
        AND deleted = 0
      ORDER BY joined_at DESC
      LIMIT 1
      """)
  PracticalCollectionQueueEntity selectActiveByCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId
  );

  @Select("""
      SELECT COALESCE(MAX(queue_no), 0)
      FROM certification_practical_collection_queue
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND deleted = 0
      """)
  Integer selectMaxQueueNo(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

  @Select("""
      SELECT *
      FROM certification_practical_collection_queue
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND queue_status = 'QUEUED'
        AND deleted = 0
      ORDER BY queue_no ASC, joined_at ASC, id ASC
      LIMIT 1 FOR UPDATE
      """)
  PracticalCollectionQueueEntity lockNextQueued(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId
  );

  @Update("""
      UPDATE certification_practical_collection_queue
      SET queue_status = 'REMOVED',
          removed_at = NOW(),
          remove_reason = #{reason},
          operator_id = #{operatorId},
          updated_at = NOW(),
          updated_by = #{operatorId}
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND candidate_id = #{candidateId}
        AND queue_status IN ('QUEUED', 'COLLECTING')
        AND deleted = 0
      """)
  int removeActiveByCandidate(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("candidateId") Long candidateId,
      @Param("operatorId") Long operatorId,
      @Param("reason") String reason
  );
}
