package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPracticalAssignmentMapper extends BaseMapper<CompetitionPracticalAssignmentEntity> {
  @Select("SELECT DISTINCT tenant_id FROM competition_practical_assignment "
      + "WHERE status = 'in_progress' AND deadline_at IS NOT NULL AND deadline_at <= #{now} "
      + "AND exam_ended_at IS NULL AND deleted = 0")
  @InterceptorIgnore(tenantLine = "true")
  List<Long> selectTenantIdsForExpiredPracticalRuntime(@Param("now") LocalDateTime now);
}
