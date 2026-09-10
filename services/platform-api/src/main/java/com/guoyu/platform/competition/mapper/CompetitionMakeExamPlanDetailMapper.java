package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanDetailEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionMakeExamPlanDetailMapper
    extends BaseMapper<CompetitionMakeExamPlanDetailEntity> {

  @Select("SELECT * FROM competition_make_exam_plan_detail "
      + "WHERE tenant_id = #{tenantId} AND plan_id = #{planId} AND deleted = 0 "
      + "ORDER BY sort_order, id")
  List<CompetitionMakeExamPlanDetailEntity> selectByPlan(
      @Param("tenantId") Long tenantId, @Param("planId") Long planId);
}
