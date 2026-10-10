package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanTopicRuleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionMakeExamPlanTopicRuleMapper extends BaseMapper<CompetitionMakeExamPlanTopicRuleEntity> {

  @Select("SELECT * FROM competition_make_exam_plan_topic_rule "
      + "WHERE tenant_id = #{tenantId} AND plan_id = #{planId} AND deleted = 0 "
      + "ORDER BY sort_order, id")
  List<CompetitionMakeExamPlanTopicRuleEntity> selectByPlan(
      @Param("tenantId") Long tenantId, @Param("planId") Long planId);
}
