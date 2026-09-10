package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Mapper for tenant-scoped competition player identities. */
public interface CompetitionPlayerIdentityMapper extends BaseMapper<CompetitionPlayerIdentityEntity> {

  @Select("""
      SELECT *
      FROM competition_player_identity
      WHERE deleted = 0
        AND (id_card_lookup_hash = #{idCardLookupHash} OR mobile_lookup_hash = #{mobileLookupHash})
      FOR UPDATE
      """)
  @Results({
      @Result(column = "id_card_ciphertext", property = "idCardNo"),
      @Result(column = "mobile_ciphertext", property = "mobile")
  })
  List<CompetitionPlayerIdentityEntity> selectForUpdateByIdentityHashes(
      @Param("idCardLookupHash") String idCardLookupHash,
      @Param("mobileLookupHash") String mobileLookupHash);

  @Select("SELECT * FROM competition_player_identity WHERE id = #{playerId} AND deleted = 0 FOR UPDATE")
  @Results({
      @Result(column = "id_card_ciphertext", property = "idCardNo"),
      @Result(column = "mobile_ciphertext", property = "mobile")
  })
  CompetitionPlayerIdentityEntity selectForUpdateById(@Param("playerId") Long playerId);

  @Select("SELECT * FROM competition_player_identity WHERE mobile_lookup_hash = #{mobileLookupHash} "
      + "AND deleted = 0 LIMIT 1 FOR UPDATE")
  @Results({
      @Result(column = "id_card_ciphertext", property = "idCardNo"),
      @Result(column = "mobile_ciphertext", property = "mobile")
  })
  CompetitionPlayerIdentityEntity selectForUpdateByMobileLookupHash(
      @Param("mobileLookupHash") String mobileLookupHash);

  /**
   * The credential-login endpoint is the sole public path allowed to locate a player without a
   * tenant header. The global active-mobile unique key guarantees this returns at most one row.
  */
  @InterceptorIgnore(tenantLine = "true")
  @Select("SELECT * FROM competition_player_identity WHERE mobile_lookup_hash = #{mobileLookupHash} "
      + "AND deleted = 0 LIMIT 2 FOR UPDATE")
  @Results({
      @Result(column = "id_card_ciphertext", property = "idCardNo"),
      @Result(column = "mobile_ciphertext", property = "mobile")
  })
  List<CompetitionPlayerIdentityEntity> selectForUpdateByMobileLookupHashGlobally(
      @Param("mobileLookupHash") String mobileLookupHash);

  /** Updates a legacy empty password hash for the identity selected by the controlled global lookup. */
  @InterceptorIgnore(tenantLine = "true")
  @Update("UPDATE competition_player_identity SET password_hash = #{passwordHash}, "
      + "password_initialized_at = #{initializedAt}, updated_at = #{initializedAt} "
      + "WHERE id = #{playerId} AND tenant_id = #{tenantId} AND deleted = 0 "
      + "AND (password_hash IS NULL OR password_hash = '')")
  int initializePasswordIfAbsentGlobally(
      @Param("playerId") Long playerId,
      @Param("tenantId") Long tenantId,
      @Param("passwordHash") String passwordHash,
      @Param("initializedAt") LocalDateTime initializedAt);
}
