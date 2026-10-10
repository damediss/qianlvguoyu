package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CertificationTaskMapper extends BaseMapper<CertificationTaskEntity> {

  @Select("""
      <script>
      SELECT
        base.task_id AS taskId,
        COALESCE(c.candidate_count, 0) AS candidateCount,
        COALESCE(d.required_doc_count, 0) AS requiredDocCount,
        COALESCE(d.approved_doc_count, 0) AS approvedDocCount,
        COALESCE(m.material_generated_count, 0) AS materialGeneratedCount,
        COALESCE(s.site_count, 0) AS siteCount,
        COALESCE(st.staff_count, 0) AS staffCount,
        COALESCE(a.assessor_count, 0) AS assessorCount
      FROM (
        SELECT id AS task_id
        FROM certification_task
        WHERE id IN
        <foreach collection="taskIds" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
      ) base
      LEFT JOIN (
        SELECT task_id, COUNT(*) AS candidate_count
        FROM certification_candidate
        WHERE deleted = 0
          AND status = 'active'
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) c ON c.task_id = base.task_id
      LEFT JOIN (
        SELECT
          task_id,
          SUM(CASE WHEN required = 1 THEN 1 ELSE 0 END) AS required_doc_count,
          SUM(CASE WHEN required = 1 AND status = 'approved' THEN 1 ELSE 0 END) AS approved_doc_count
        FROM certification_candidate_document
        WHERE deleted = 0
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) d ON d.task_id = base.task_id
      LEFT JOIN (
        SELECT task_id, COUNT(*) AS material_generated_count
        FROM certification_application_material_batch
        WHERE deleted = 0
          AND batch_type = 'generate'
          AND status = 'generated'
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) m ON m.task_id = base.task_id
      LEFT JOIN (
        SELECT task_id, COUNT(*) AS site_count
        FROM certification_task_exam_site
        WHERE deleted = 0
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) s ON s.task_id = base.task_id
      LEFT JOIN (
        SELECT task_id, COUNT(*) AS staff_count
        FROM certification_task_exam_staff
        WHERE deleted = 0
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) st ON st.task_id = base.task_id
      LEFT JOIN (
        SELECT task_id, COUNT(*) AS assessor_count
        FROM certification_task_assessor
        WHERE deleted = 0
          AND task_id IN
          <foreach collection="taskIds" item="id" open="(" separator="," close=")">
            #{id}
          </foreach>
        GROUP BY task_id
      ) a ON a.task_id = base.task_id
      </script>
      """)
  List<Map<String, Object>> selectPreparationTaskOverviewStats(@Param("taskIds") List<Long> taskIds);
}
