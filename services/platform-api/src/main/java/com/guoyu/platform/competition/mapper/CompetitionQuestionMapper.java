package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionQuestionMapper extends BaseMapper<CompetitionQuestionEntity> {

  /**
   * Returns the largest question number in the complete event/work/level/content scope.
   *
   * <p>The questionType argument remains for source compatibility with the original mapper
   * contract; numbering for newly created questions is intentionally shared by all question
   * types in that scope.</p>
   */
  @Select("SELECT MAX(sort_order) FROM competition_question "
      + "WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
      + "AND event_work_type_id = #{eventWorkTypeId} "
      + "AND event_work_type_level_id = #{eventWorkTypeLevelId} "
      + "AND content_type = #{contentType} "
      + "AND deleted = 0")
  Integer selectMaxSortOrder(
      @Param("tenantId") Long tenantId,
      @Param("eventId") Long eventId,
      @Param("eventWorkTypeId") Long eventWorkTypeId,
      @Param("eventWorkTypeLevelId") Long eventWorkTypeLevelId,
      @Param("contentType") String contentType,
      // Kept in the mapper contract for callers compiled against the original method.  New
      // question creation deliberately calculates the next number across all question types.
      @Param("questionType") String questionType);
}
