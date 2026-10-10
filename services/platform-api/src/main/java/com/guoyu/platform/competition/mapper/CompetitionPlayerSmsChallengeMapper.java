package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPlayerSmsChallengeEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionPlayerSmsChallengeMapper extends BaseMapper<CompetitionPlayerSmsChallengeEntity> {

  @Select("SELECT * FROM competition_player_sms_challenge WHERE id = #{challengeId} AND deleted = 0 LIMIT 1 FOR UPDATE")
  CompetitionPlayerSmsChallengeEntity selectForUpdateById(@Param("challengeId") Long challengeId);

  @Select("""
      SELECT *
      FROM competition_player_sms_challenge
      WHERE id = #{challengeId}
        AND player_id = #{playerId}
        AND purpose = #{purpose}
        AND deleted = 0
      FOR UPDATE
      """)
  CompetitionPlayerSmsChallengeEntity selectForUpdate(
      @Param("challengeId") Long challengeId,
      @Param("playerId") Long playerId,
      @Param("purpose") String purpose);

  @Select("""
      SELECT *
      FROM competition_player_sms_challenge
      WHERE player_id = #{playerId}
        AND purpose = #{purpose}
        AND status = 'locked'
        AND locked_until > #{now}
        AND deleted = 0
      ORDER BY locked_until DESC, id DESC
      LIMIT 1
      FOR UPDATE
      """)
  CompetitionPlayerSmsChallengeEntity selectActiveLockForUpdate(
      @Param("playerId") Long playerId,
      @Param("purpose") String purpose,
      @Param("now") LocalDateTime now);
}
