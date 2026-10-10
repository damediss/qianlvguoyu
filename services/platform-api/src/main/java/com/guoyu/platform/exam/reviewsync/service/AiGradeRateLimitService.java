package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * AI 判分限流（《简答题作答与AI判分方案》§5-10 / Q-D）。
 *
 * <p>收敛在判分服务层、两链共用（/appreviewai/grade 与 answer/mock 内部判分同一计次口径）。
 * 双维度：每用户每分钟 + 每用户每日，阈值走 sys_dict（ai_review_grade_rate_minute 默认 10、
 * ai_review_grade_rate_day 默认 200）。Lua 原子「检查 → INCR → 惰性 EXPIRE」，仿
 * {@code CompetitionPlayerSmsRateLimitService} 写法。
 *
 * <p>计次时机：缓存 miss 之后、真实 LLM 调用之前（由 {@link AiGradeApplicationService} 编排），
 * 缓存命中零成本不计次，避免重复练习同题的学员被误限。
 */
@Slf4j
@Service
public class AiGradeRateLimitService {

  private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
  private static final DateTimeFormatter MINUTE_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
  private static final String DICT_RATE_MINUTE = "ai_review_grade_rate_minute";
  private static final String DICT_RATE_DAY = "ai_review_grade_rate_day";
  private static final int DEFAULT_RATE_MINUTE = 10;
  private static final int DEFAULT_RATE_DAY = 200;
  /** 分钟桶 TTL 覆盖 2 个桶周期，日桶与 SMS 限流同口径 2 天 */
  private static final long MINUTE_TTL_SECONDS = 120L;
  private static final long DAY_TTL_SECONDS = 172800L;

  private static final DefaultRedisScript<Long> CHECK_AND_ACQUIRE = new DefaultRedisScript<>("""
      local minuteCount = tonumber(redis.call('GET', KEYS[1]) or '0')
      local dayCount = tonumber(redis.call('GET', KEYS[2]) or '0')
      local minuteLimit = tonumber(ARGV[1])
      local dayLimit = tonumber(ARGV[2])
      if minuteCount >= minuteLimit or dayCount >= dayLimit then
        return -1
      end
      if redis.call('INCR', KEYS[1]) == 1 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end
      if redis.call('INCR', KEYS[2]) == 1 then redis.call('EXPIRE', KEYS[2], ARGV[4]) end
      return 1
      """, Long.class);

  private final StringRedisTemplate redisTemplate;
  private final DictMapper dictMapper;

  public AiGradeRateLimitService(StringRedisTemplate redisTemplate, DictMapper dictMapper) {
    this.redisTemplate = redisTemplate;
    this.dictMapper = dictMapper;
  }

  /** 计次一次；超限抛 TOO_MANY_REQUESTS（前端据错误码转自评兜底） */
  public void acquire(Long tenantId, Long userId) {
    int minuteLimit = dictInt(DICT_RATE_MINUTE, DEFAULT_RATE_MINUTE);
    int dayLimit = dictInt(DICT_RATE_DAY, DEFAULT_RATE_DAY);
    LocalDateTime now = LocalDateTime.now(CHINA_ZONE);
    String minuteKey = "grade:rate:min:" + tenantId + ":" + userId + ":" + MINUTE_BUCKET.format(now);
    String dayKey = "grade:rate:day:" + tenantId + ":" + userId + ":" + LocalDate.now(CHINA_ZONE);
    try {
      Long result = redisTemplate.execute(CHECK_AND_ACQUIRE,
          List.of(minuteKey, dayKey),
          String.valueOf(minuteLimit),
          String.valueOf(dayLimit),
          String.valueOf(MINUTE_TTL_SECONDS),
          String.valueOf(DAY_TTL_SECONDS));
      if (result != null && result == 1L) {
        return;
      }
      log.info("[AI判分] 限流触发：tenantId={} userId={} minuteLimit={} dayLimit={}",
          tenantId, userId, minuteLimit, dayLimit);
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "AI 判分次数已达上限，请自评");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      // 与 SMS 限流同口径：Redis 故障 fail-closed，前端转自评兜底
      log.warn("[AI判分] 限流服务异常：tenantId={} userId={} error={}", tenantId, userId, ex.getMessage());
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 判分暂不可用，请自评");
    }
  }

  private int dictInt(String code, int defaultValue) {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, code)
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && dicts.get(0).getValue() != null) {
        return Integer.parseInt(dicts.get(0).getValue().trim());
      }
    } catch (Exception e) {
      log.warn("[AI判分] 读取字典 {} 失败，用默认值 {}：{}", code, defaultValue, e.getMessage());
    }
    return defaultValue;
  }
}
