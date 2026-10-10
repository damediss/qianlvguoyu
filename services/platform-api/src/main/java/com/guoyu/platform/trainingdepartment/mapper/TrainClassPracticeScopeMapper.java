package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeScopeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainClassPracticeScopeMapper extends BaseMapper<TrainClassPracticeScopeEntity> {

  /**
   * 删班清理时物理删除，避免 uk_class_practice_scope(tenant_id, class_id) 被软删行占住。
   */
  @Delete("""
      DELETE FROM train_class_practice_scope
      WHERE tenant_id = #{tenantId}
        AND class_id = #{classId}
      """)
  int physicalDeleteByClass(@Param("tenantId") Long tenantId, @Param("classId") Long classId);
}
