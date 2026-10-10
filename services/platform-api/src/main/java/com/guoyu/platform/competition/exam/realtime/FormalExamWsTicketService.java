package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamWsTicketService {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final FormalExamRealtimeProperties properties;

  public FormalExamWsTicketService(
      @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redis,
      ObjectMapper objectMapper,
      FormalExamRealtimeProperties properties) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public record Ticket(
      Long tenantId,
      Long playerId,
      Long accessSessionId,
      Integer generation,
      Long examSessionId,
      Long candidateExamId,
      Instant expiresAt) {
  }

  public record ConsoleTicket(
      Long tenantId,
      Long userId,
      Long examSessionId,
      Instant expiresAt) {
  }

  public String issue(Ticket ticket) {
    requireRedis();
    if (ticket == null || ticket.accessSessionId() == null || ticket.generation() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
    }
    String value = UUID.randomUUID().toString();
    String key = FormalExamRealtimeTypes.TICKET_KEY_PREFIX + value;
    try {
      Boolean stored = redis.opsForValue().setIfAbsent(
          key, objectMapper.writeValueAsString(ticket), Duration.ofSeconds(ttlSeconds()));
      if (!Boolean.TRUE.equals(stored)) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实时通道票据创建失败");
      }
      return value;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("Formal exam WS ticket issue failed, accessSessionId={}", ticket.accessSessionId());
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
  }

  public Ticket consume(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    requireRedis();
    try {
      String json = redis.opsForValue().getAndDelete(FormalExamRealtimeTypes.TICKET_KEY_PREFIX + value.trim());
      if (!StringUtils.hasText(json)) {
        return null;
      }
      Ticket ticket = objectMapper.readValue(json, Ticket.class);
      if (ticket.expiresAt() != null && ticket.expiresAt().isBefore(Instant.now())) {
        return null;
      }
      return ticket;
    } catch (JsonProcessingException ex) {
      log.warn("Formal exam WS ticket payload invalid");
      return null;
    } catch (RuntimeException ex) {
      log.warn("Formal exam WS ticket consume failed");
      return null;
    }
  }

  public String issueConsole(ConsoleTicket ticket) {
    requireRedis();
    if (ticket == null || ticket.examSessionId() == null || ticket.userId() == null || ticket.tenantId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理端实时通道票据无效");
    }
    String value = UUID.randomUUID().toString();
    String key = FormalExamRealtimeTypes.CONSOLE_TICKET_KEY_PREFIX + value;
    try {
      Boolean stored = redis.opsForValue().setIfAbsent(
          key, objectMapper.writeValueAsString(ticket), Duration.ofSeconds(ttlSeconds()));
      if (!Boolean.TRUE.equals(stored)) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实时通道票据创建失败");
      }
      return value;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("Formal exam console WS ticket issue failed, examSessionId={}", ticket.examSessionId());
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
  }

  public ConsoleTicket consumeConsole(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    requireRedis();
    try {
      String json = redis.opsForValue().getAndDelete(
          FormalExamRealtimeTypes.CONSOLE_TICKET_KEY_PREFIX + value.trim());
      if (!StringUtils.hasText(json)) {
        return null;
      }
      ConsoleTicket ticket = objectMapper.readValue(json, ConsoleTicket.class);
      if (ticket.expiresAt() != null && ticket.expiresAt().isBefore(Instant.now())) {
        return null;
      }
      return ticket;
    } catch (JsonProcessingException ex) {
      log.warn("Formal exam console WS ticket payload invalid");
      return null;
    } catch (RuntimeException ex) {
      log.warn("Formal exam console WS ticket consume failed");
      return null;
    }
  }

  public int ttlSeconds() {
    int ttl = properties.getTicketTtlSeconds();
    return ttl < 30 ? 30 : Math.min(ttl, 60);
  }

  private void requireRedis() {
    if (redis == null) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
  }
}
