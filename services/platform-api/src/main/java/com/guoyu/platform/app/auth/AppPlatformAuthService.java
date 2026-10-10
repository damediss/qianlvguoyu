package com.guoyu.platform.app.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.entity.PlatformUserTokenEntity;
import com.guoyu.platform.app.mapper.PlatformUserTokenMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 对齐 PHP ControllerBase::app_user_by_token */
@Service
public class AppPlatformAuthService {

  private final PlatformUserTokenMapper tokenMapper;
  private final PlatformUserMapper platformUserMapper;

  public AppPlatformAuthService(
      PlatformUserTokenMapper tokenMapper,
      PlatformUserMapper platformUserMapper) {
    this.tokenMapper = tokenMapper;
    this.platformUserMapper = platformUserMapper;
  }

  public AppPlatformUser resolveUser(String token) {
    if (!StringUtils.hasText(token)) {
      return null;
    }
    PlatformUserTokenEntity tokenEntity = tokenMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserTokenEntity>()
            .eq(PlatformUserTokenEntity::getToken, token.trim())
            .last("LIMIT 1"));
    if (tokenEntity == null || tokenEntity.getUserId() == null) {
      return null;
    }
    PlatformUserEntity user = platformUserMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserEntity>()
            .eq(PlatformUserEntity::getId, tokenEntity.getUserId())
            .eq(PlatformUserEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (user == null) {
      return null;
    }
    return new AppPlatformUser(
        user.getId(),
        user.getRealname(),
        user.getPhone(),
        user.getIdcard(),
        user.getExpertId());
  }
}
