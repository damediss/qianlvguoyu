package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Mapper for tenant-scoped competition player identities. */
public interface CompetitionPlayerIdentityMapper extends BaseMapper<CompetitionPlayerIdentityEntity> {

  @Select("""
      SELECT *
      FROM competition_player_identity
      WHERE deleted = 0
        AND (id_card_lookup_hash = #{idCardLookupHash} OR mobile_lookup_hash = #{mobileLookupHash})
      FOR UPDATE
      """)
  List<CompetitionPlayerIdentityEntity> selectForUpdateByIdentityHashes(
      @Param("idCardLookupHash") String idCardLookupHash,
      @Param("mobileLookupHash") String mobileLookupHash);

  @Select("SELECT * FROM competition_player_identity WHERE id = #{playerId} AND deleted = 0 FOR UPDATE")
  CompetitionPlayerIdentityEntity selectForUpdateById(@Param("playerId") Long playerId);

  @Select("SELECT * FROM competition_player_identity WHERE mobile_lookup_hash = #{mobileLookupHash} "
      + "AND deleted = 0 LIMIT 1 FOR UPDATE")
  CompetitionPlayerIdentityEntity selectForUpdateByMobileLookupHash(
      @Param("mobileLookupHash") String mobileLookupHash);
}
