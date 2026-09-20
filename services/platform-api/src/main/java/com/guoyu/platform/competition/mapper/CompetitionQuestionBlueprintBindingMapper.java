package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionQuestionBlueprintBindingMapper extends BaseMapper<CompetitionQuestionBlueprintBindingEntity> {

  @Select("SELECT * FROM competition_question_blueprint_binding "
      + "WHERE tenant_id = #{tenantId} AND blueprint_id = #{blueprintId} AND deleted = 0")
  List<CompetitionQuestionBlueprintBindingEntity> selectByBlueprint(
      @Param("tenantId") Long tenantId, @Param("blueprintId") Long blueprintId);

  @Select("SELECT * FROM competition_question_blueprint_binding "
      + "WHERE tenant_id = #{tenantId} AND question_id = #{questionId} "
      + "AND blueprint_id = #{blueprintId} AND deleted = 0 ORDER BY id DESC")
  List<CompetitionQuestionBlueprintBindingEntity> selectByQuestionAndBlueprint(
      @Param("tenantId") Long tenantId, @Param("questionId") Long questionId,
      @Param("blueprintId") Long blueprintId);
}
