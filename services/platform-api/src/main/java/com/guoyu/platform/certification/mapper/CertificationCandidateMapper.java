package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CertificationCandidateMapper extends BaseMapper<CertificationCandidateEntity> {

  @Select("""
      SELECT id, tenant_id, task_id, candidate_no, candidate_name, id_card_no, masked_id_card_no, phone, masked_phone,
             level_code, level_name, work_type_id, work_type_name, import_batch_id, import_source, org_name,
             document_completed_count, document_required_count, document_complete, status, remark,
             created_at, updated_at, created_by, updated_by, deleted
      FROM certification_candidate
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND id_card_no = #{idCardNo}
      ORDER BY deleted ASC, created_at DESC
      LIMIT 1
      """)
  CertificationCandidateEntity selectByTaskAndIdCardIncludingDeleted(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("idCardNo") String idCardNo
  );

  @Delete("""
      DELETE FROM certification_candidate
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND id_card_no = #{idCardNo}
        AND deleted = 1
      """)
  int purgeSoftDeletedByTaskAndIdCard(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("idCardNo") String idCardNo
  );

  @Delete("DELETE FROM certification_candidate WHERE id = #{id}")
  int physicalDeleteById(@Param("id") Long id);

  @Update("""
      UPDATE certification_candidate
      SET candidate_no = #{entity.candidateNo},
          candidate_name = #{entity.candidateName},
          id_card_no = #{entity.idCardNo},
          masked_id_card_no = #{entity.maskedIdCardNo},
          phone = #{entity.phone},
          masked_phone = #{entity.maskedPhone},
          level_code = #{entity.levelCode},
          level_name = #{entity.levelName},
          work_type_id = #{entity.workTypeId},
          work_type_name = #{entity.workTypeName},
          import_batch_id = #{entity.importBatchId},
          import_source = #{entity.importSource},
          org_name = #{entity.orgName},
          document_completed_count = #{entity.documentCompletedCount},
          document_required_count = #{entity.documentRequiredCount},
          document_complete = #{entity.documentComplete},
          status = #{entity.status},
          remark = #{entity.remark},
          deleted = 0,
          updated_at = NOW()
      WHERE id = #{entity.id}
      """)
  int restoreSoftDeleted(@Param("entity") CertificationCandidateEntity entity);
}
