package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionQuestionMapper extends BaseMapper<CompetitionQuestionEntity> {

  @Select("SELECT MAX(sort_order) FROM competition_question "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND event_work_type_id = #{eventWorkTypeId} "
      + "AND event_work_type_level_id = #{eventWorkTypeLevelId} "
      + "AND content_type = #{contentType} AND question_type = #{questionType} "
      + "AND deleted = 0")
  Integer selectMaxSortOrder(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("contentType") String contentType,
      @Param("questionType") String questionType);
}
