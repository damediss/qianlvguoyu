package com.guoyu.platform.certification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.certification.entity.CertificationTaskChecklistItemEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CertificationTaskChecklistItemMapper extends BaseMapper<CertificationTaskChecklistItemEntity> {

  @Update("""
      UPDATE certification_task_checklist_item
      SET checked = 0,
          confirmer_task_staff_id = NULL,
          confirmer_name = NULL,
          confirmed_at = NULL,
          updated_at = NOW()
      WHERE task_id = #{taskId}
        AND deleted = 0
      """)
  int clearConfirmation(@Param("taskId") Long taskId);
}
