package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * 解析生成每学员每日限流（《试题解析Markdown表格化_一期实施方案》§5.4 步骤 9 / §5.9）。
 *
 * <p>key={@code resolve:rate:day:{tenantId}:{userId}:{yyyy-MM-dd}}，阈值走 sys_dict
 * {@code question_resolve_daily_limit}（默认 20）。Lua 原子「检查 → INCR → 惰性 EXPIRE」，
 * 仿 {@link com.guoyu.platform.exam.reviewsync.service.AiGradeRateLimitService}。
 *
 * <p>计次时机：缓存 miss、锁竞争、冷却命中均不计次，仅真实 LLM 调用前由编排方调用一次
 * （App 链路）；Redis 异常 fail-closed 返回 rate_limited，不无限放行。
 */
@Slf4j
@Service
public class QuestionResolveRateLimitService {

  private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
  private static final String DICT_DAILY_LIMIT = "question_resolve_daily_limit";
  private static final int DEFAULT_DAILY_LIMIT = 20;
  /** 日桶 TTL 与判分限流同口径 2 天，跨自然日惰性过期 */
  private static final long DAY_TTL_SECONDS = 172800L;

  private static final DefaultRedisScript<Long> CHECK_AND_ACQUIRE = new DefaultRedisScript<>("""
      local dayCount = tonumber(redis.call('GET', KEYS[1]) or '0')
      local dayLimit = tonumber(ARGV[1])
      if dayCount >= dayLimit then
        return -1
      end
      if redis.call('INCR', KEYS[1]) == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
      return 1
      """, Long.class);

  private final StringRedisTemplate redisTemplate;
  private final DictMapper dictMapper;

  public QuestionResolveRateLimitService(StringRedisTemplate redisTemplate, DictMapper dictMapper) {
    this.redisTemplate = redisTemplate;
    this.dictMapper = dictMapper;
  }

  /** 计次一次；超限或 Redis 异常抛 TOO_MANY_REQUESTS（编排方转为 empty/rate_limited）。 */
  public void acquire(Long tenantId, Long userId) {
    int dayLimit = dictInt(DICT_DAILY_LIMIT, DEFAULT_DAILY_LIMIT);
    String dayKey = "resolve:rate:day:" + tenantId + ":" + userId + ":"
        + LocalDate.now(CHINA_ZONE);
    try {
      Long result = redisTemplate.execute(CHECK_AND_ACQUIRE,
          List.of(dayKey), String.valueOf(dayLimit), String.valueOf(DAY_TTL_SECONDS));
      if (result != null && result == 1L) {
        return;
      }
      log.info("[题目解析] 限流触发：tenantId={} userId={} dayLimit={}", tenantId, userId, dayLimit);
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "解析生成次数已达上限，请明天再试");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("[题目解析] 限流服务异常：tenantId={} userId={} error={}", tenantId, userId, ex.getMessage());
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "解析生成暂不可用，请稍后再试");
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
      log.warn("[题目解析] 读取字典 {} 失败，用默认值 {}：{}", code, defaultValue, e.getMessage());
    }
    return defaultValue;
  }
}
