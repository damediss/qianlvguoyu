package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainScheduleMapper extends BaseMapper<TrainScheduleEntity> {

  /** 全量替换课表时物理删除，避免软删堆积 */
  @Delete("""
      DELETE FROM train_schedule
      WHERE class_id = #{classId}
        AND tenant_id = #{tenantId}
      """)
  int physicalDeleteByClass(@Param("classId") Long classId, @Param("tenantId") Long tenantId);
}
