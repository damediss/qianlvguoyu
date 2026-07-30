package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CertificationCandidateMapper extends BaseMapper<CertificationCandidateEntity> {

  @Select("""
      SELECT id, tenant_id, task_id, task_scope_id, candidate_no, admission_ticket_no, candidate_name, id_card_no, masked_id_card_no, phone, masked_phone,
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

  @Select("""
      SELECT id, tenant_id, task_id, task_scope_id, candidate_no, admission_ticket_no, candidate_name, id_card_no, masked_id_card_no, phone, masked_phone,
             level_code, level_name, work_type_id, work_type_name, import_batch_id, import_source, org_name,
             document_completed_count, document_required_count, document_complete, status, remark,
             created_at, updated_at, created_by, updated_by, deleted
      FROM certification_candidate
      WHERE tenant_id = #{tenantId}
        AND task_id = #{taskId}
        AND admission_ticket_no = #{admissionTicketNo}
      ORDER BY deleted ASC, created_at DESC
      LIMIT 1
      """)
  CertificationCandidateEntity selectByTaskAndAdmissionTicketIncludingDeleted(
      @Param("tenantId") Long tenantId,
      @Param("taskId") Long taskId,
      @Param("admissionTicketNo") String admissionTicketNo
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
          admission_ticket_no = #{entity.admissionTicketNo},
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

  @Select("""
      <script>
      SELECT d.candidate_id AS candidateId,
             SUM(CASE WHEN d.required = 1 THEN 1 ELSE 0 END) AS requiredCount,
             SUM(CASE WHEN d.required = 1 AND d.status = 'approved' THEN 1 ELSE 0 END) AS approvedCount
      FROM certification_candidate_document d
      WHERE d.task_id = #{taskId}
        AND d.deleted = 0
        AND d.candidate_id IN
        <foreach item="cid" collection="candidateIds" open="(" separator="," close=")">#{cid}</foreach>
      GROUP BY d.candidate_id
      </script>
      """)
  @MapKey("candidateId")
  Map<Long, Map<String, Object>> selectDocumentStatsByCandidate(
      @Param("taskId") Long taskId,
      @Param("candidateIds") List<Long> candidateIds);

  @Update("""
      <script>
      UPDATE certification_candidate c
      JOIN (
        SELECT d.candidate_id AS cid,
               SUM(CASE WHEN d.required = 1 THEN 1 ELSE 0 END) AS required_count,
               SUM(CASE WHEN d.required = 1 AND d.status = 'approved' THEN 1 ELSE 0 END) AS approved_count
        FROM certification_candidate_document d
        WHERE d.task_id = #{taskId}
          AND d.deleted = 0
          AND d.required = 1
        GROUP BY d.candidate_id
      ) s ON c.id = s.cid
      SET c.document_required_count = s.required_count,
          c.document_completed_count = s.approved_count,
          c.document_complete = (s.required_count > 0 AND s.required_count = s.approved_count),
          c.updated_at = NOW()
      WHERE c.task_id = #{taskId}
        AND c.deleted = 0
        AND c.id IN
        <foreach item="cid" collection="candidateIds" open="(" separator="," close=")">#{cid}</foreach>
      </script>
      """)
  int batchRefreshDocumentStats(
      @Param("taskId") Long taskId,
      @Param("candidateIds") List<Long> candidateIds);
}
