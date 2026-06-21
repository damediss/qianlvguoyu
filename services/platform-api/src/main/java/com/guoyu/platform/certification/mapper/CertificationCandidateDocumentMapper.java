package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CertificationCandidateDocumentMapper extends BaseMapper<CertificationCandidateDocumentEntity> {

  @Delete("""
      DELETE FROM certification_candidate_document
      WHERE task_id = #{taskId}
        AND candidate_id = #{candidateId}
      """)
  int physicalDeleteByCandidate(@Param("taskId") Long taskId, @Param("candidateId") Long candidateId);

  @Select("""
      <script>
      SELECT candidate_id, doc_code
      FROM certification_candidate_document
      WHERE task_id = #{taskId}
        AND deleted = 0
        AND candidate_id IN
        <foreach item="cid" collection="candidateIds" open="(" separator="," close=")">#{cid}</foreach>
      </script>
      """)
  List<CandidateDocRow> selectExistingCandidateDocCodes(
      @Param("taskId") Long taskId,
      @Param("candidateIds") List<Long> candidateIds);

  @Insert("""
      <script>
      INSERT INTO certification_candidate_document
        (id, tenant_id, task_id, candidate_id, template_id, doc_code, doc_name,
         required, version_no, status, deleted, created_at, updated_at)
      VALUES
      <foreach item="row" collection="rows" separator=",">
        (#{row.id}, #{row.tenantId}, #{row.taskId}, #{row.candidateId}, #{row.templateId},
         #{row.docCode}, #{row.docName}, #{row.required}, 1, 'missing', 0, NOW(), NOW())
      </foreach>
      </script>
      """)
  int batchInsertMissing(
      @Param("rows") List<CandidateDocRow> rows);
}
