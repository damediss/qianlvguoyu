package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionTheoryAnswerMapper extends BaseMapper<CompetitionTheoryAnswerEntity> {
  @Select("SELECT * FROM competition_theory_answer WHERE attempt_id = #{attemptId} "
      + "AND question_snapshot_id = #{questionId} AND deleted = 0 LIMIT 1 FOR UPDATE")
  CompetitionTheoryAnswerEntity selectForUpdate(
      @Param("attemptId") Long attemptId,
      @Param("questionId") Long questionId);

  @Select("SELECT * FROM competition_theory_answer WHERE attempt_id = #{attemptId} "
      + "AND deleted = 0 ORDER BY question_snapshot_id")
  List<CompetitionTheoryAnswerEntity> selectByAttempt(@Param("attemptId") Long attemptId);
}
