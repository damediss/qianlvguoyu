package com.guoyu.platform.competition.player.sms;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;
import com.guoyu.platform.competition.player.config.CompetitionPlayerSmsProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class CompetitionPlayerSmsRateLimitService {

  private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
  private static final DefaultRedisScript<Long> CHECK_AND_ACQUIRE = new DefaultRedisScript<>("""
      if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) == false then return 0 end
      local playerCount = tonumber(redis.call('GET', KEYS[2]) or '0')
      local mobileCount = tonumber(redis.call('GET', KEYS[3]) or '0')
      local playerLimit = tonumber(ARGV[2])
      local mobileLimit = tonumber(ARGV[3])
      if playerCount >= playerLimit or mobileCount >= mobileLimit then
        redis.call('DEL', KEYS[1])
        return -1
      end
      redis.call('INCR', KEYS[2])
      redis.call('EXPIRE', KEYS[2], ARGV[4])
      redis.call('INCR', KEYS[3])
      redis.call('EXPIRE', KEYS[3], ARGV[4])
      return 1
      """, Long.class);

  private final StringRedisTemplate redisTemplate;
  private final CompetitionPlayerSmsProperties properties;

  public CompetitionPlayerSmsRateLimitService(
      StringRedisTemplate redisTemplate,
      CompetitionPlayerSmsProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  public void acquire(Long tenantId, Long playerId, String mobileLookupHash, CompetitionPlayerSmsPurpose purpose) {
    LocalDate day = LocalDate.now(CHINA_ZONE);
    String suffix = tenantId + ":" + purpose.getCode() + ":" + mobileLookupHash;
    String cooldownKey = "competition:player:sms:cooldown:" + suffix;
    String playerDailyKey = "competition:player:sms:daily:player:" + day + ":" + tenantId + ":" + playerId;
    String mobileDailyKey = "competition:player:sms:daily:mobile:" + day + ":" + suffix;
    long dayTtlSeconds = 172800L;
    try {
      Long result = redisTemplate.execute(CHECK_AND_ACQUIRE,
          List.of(cooldownKey, playerDailyKey, mobileDailyKey),
          String.valueOf(properties.getResendCooldownSeconds()),
          String.valueOf(properties.getDailyLimitPerPlayer()),
          String.valueOf(properties.getDailyLimitPerMobile()),
          String.valueOf(dayTtlSeconds));
      if (result != null && result == 1L) {
        return;
      }
      if (result != null && result == 0L) {
        throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
      }
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "今日验证码发送次数已达上限");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "验证码服务暂不可用");
    }
  }
}
