package com.guoyu.platform.competition.draw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawAssignmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionPlayerDrawAssignmentMapper
    extends BaseMapper<CompetitionPlayerDrawAssignmentEntity> {

  @Select("SELECT * FROM competition_player_draw_assignment "
      + "WHERE task_id = #{taskId} AND deleted = 0 ORDER BY created_at ASC, id ASC")
  List<CompetitionPlayerDrawAssignmentEntity> selectByTaskId(@Param("taskId") Long taskId);

  @Select("SELECT * FROM competition_player_draw_assignment "
      + "WHERE task_id = #{taskId} AND player_id = #{playerId} AND deleted = 0 "
      + "LIMIT 1 FOR UPDATE")
  CompetitionPlayerDrawAssignmentEntity selectForUpdateByTaskAndPlayer(
      @Param("taskId") Long taskId,
      @Param("playerId") Long playerId);
}
