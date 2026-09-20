package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionQuestionBlueprintNodeMapper extends BaseMapper<CompetitionQuestionBlueprintNodeEntity> {

  @Select("SELECT * FROM competition_question_blueprint_node "
      + "WHERE tenant_id = #{tenantId} AND blueprint_id = #{blueprintId} AND deleted = 0 "
      + "ORDER BY sort_order, id")
  List<CompetitionQuestionBlueprintNodeEntity> selectByBlueprint(
      @Param("tenantId") Long tenantId, @Param("blueprintId") Long blueprintId);
}
