package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationApplicationMaterialItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface CertificationApplicationMaterialItemMapper extends BaseMapper<CertificationApplicationMaterialItemEntity> {

  @Insert("""
      <script>
      INSERT INTO certification_application_material_item
        (tenant_id, task_id, batch_id, candidate_id, template_id, item_code, item_name,
         required, file_id, file_name, version_no, status, deleted, created_at, updated_at)
      VALUES
      <foreach item="item" collection="items" separator=",">
        (#{item.tenantId}, #{item.taskId}, #{item.batchId}, #{item.candidateId}, #{item.templateId},
         #{item.itemCode}, #{item.itemName}, #{item.required}, #{item.fileId}, #{item.fileName},
         1, 'generated', 0, NOW(), NOW())
      </foreach>
      </script>
      """)
  int batchInsertGenerated(@Param("items") List<CertificationApplicationMaterialItemEntity> items);
}
