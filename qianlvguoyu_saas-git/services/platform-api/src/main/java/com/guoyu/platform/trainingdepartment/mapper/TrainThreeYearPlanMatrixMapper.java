package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanMatrixEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainThreeYearPlanMatrixMapper extends BaseMapper<TrainThreeYearPlanMatrixEntity> {

  @Delete("""
      DELETE FROM train_three_year_plan_matrix
      WHERE plan_id = #{planId}
        AND tenant_id = #{tenantId}
      """)
  int physicalDeleteByPlan(@Param("planId") Long planId, @Param("tenantId") Long tenantId);
}
