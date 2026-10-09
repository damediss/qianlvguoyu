package com.guoyu.platform.auth.support;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录失败锁定（修订方案 §3.1，用户口径：只做失败锁定，验证码保持关闭）。
 *
 * <p>Redis 计数 key `login:fail:{tenantCode}:{username}`，连续失败达到阈值即锁定一段时间；
 * 锁定期间即使密码正确也拒绝；登录成功清零。失败与锁定均记日志，便于审计与告警。
 *
 * <p>Redis 不可用时的取舍：记录日志后放行（fail-open）。理由——Redis 抖动导致全员无法登录
 * 的代价高于短暂失去爆破防护；该降级会被明确记录在 WARN 日志里。
 */
@Component
@Slf4j
public class LoginFailureGuard {

  private static final String KEY_PREFIX = "login:fail:";

  private final StringRedisTemplate redisTemplate;
  private final int maxFailures;
  private final Duration lockDuration;

  public LoginFailureGuard(
      StringRedisTemplate redisTemplate,
      @Value("${platform.security.login.max-failures:5}") int maxFailures,
      @Value("${platform.security.login.lock-seconds:900}") long lockSeconds) {
    this.redisTemplate = redisTemplate;
    this.maxFailures = maxFailures;
    this.lockDuration = Duration.ofSeconds(lockSeconds);
  }

  /** 返回剩余锁定秒数；0 表示未锁定。 */
  public long remainingLockSeconds(String tenantCode, String username) {
    Long ttl = execute(() -> redisTemplate.getExpire(key(tenantCode, username)));
    return ttl == null || ttl <= 0 ? 0L : ttl;
  }

  /** 锁定时长（秒），供锁定文案换算剩余分钟。 */
  public long lockSeconds() {
    return lockDuration.getSeconds();
  }

  /** 一次失败记录的结果：当前连续失败次数与是否本次触发锁定。 */
  public record FailureOutcome(long count, boolean justLocked) {
    public static final FailureOutcome NONE = new FailureOutcome(0, false);
  }

  /**
   * 记录一次失败，返回当前连续失败次数与是否本次触发锁定。
   * Redis 不可用（fail-open）时返回 {@link FailureOutcome#NONE}。
   */
  public FailureOutcome recordFailure(String tenantCode, String username) {
    String key = key(tenantCode, username);
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, lockDuration);
      }
      log.info("登录失败计数 tenantCode={} username={} count={}", tenantCode, username, count);
      if (count != null && count >= maxFailures) {
        // 达到阈值：把 TTL 固定为锁定时长，确保从最后一次失败起算
        redisTemplate.expire(key, lockDuration);
        log.warn("登录失败达到上限，账号已锁定 tenantCode={} username={} lockSeconds={}",
            tenantCode, username, lockDuration.getSeconds());
        return new FailureOutcome(count, true);
      }
      return count == null ? FailureOutcome.NONE : new FailureOutcome(count, false);
    } catch (Exception ex) {
      log.warn("登录失败计数写入 Redis 失败，本次不锁定 tenantCode={} username={} err={}",
          tenantCode, username, ex.getMessage());
      return FailureOutcome.NONE;
    }
  }

  /** 登录成功清零。 */
  public void clear(String tenantCode, String username) {
    try {
      redisTemplate.delete(key(tenantCode, username));
    } catch (Exception ex) {
      log.warn("登录失败计数清理失败 tenantCode={} username={} err={}",
          tenantCode, username, ex.getMessage());
    }
  }

  private String key(String tenantCode, String username) {
    return KEY_PREFIX + (tenantCode == null ? "-" : tenantCode) + ":" + (username == null ? "-" : username);
  }

  private <T> T execute(java.util.function.Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      log.warn("登录锁定状态读取失败，按未锁定处理: {}", ex.getMessage());
      return null;
    }
  }
}
