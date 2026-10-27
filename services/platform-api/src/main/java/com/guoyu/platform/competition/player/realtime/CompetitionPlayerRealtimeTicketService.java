package com.guoyu.platform.competition.player.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Issues short-lived, single-use credentials for the player homepage WebSocket. */
@Service
@Slf4j
public class CompetitionPlayerRealtimeTicketService {

  private static final String TICKET_KEY_PREFIX = "competition:player:realtime:ticket:";
  private static final int TICKET_TTL_SECONDS = 60;

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerRealtimeTicketService(
      @Autowired(required = false) StringRedisTemplate redis,
      ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  public record Ticket(Long tenantId, Long playerId, Instant expiresAt) {
  }

  public String issue(Long tenantId, Long playerId) {
    requireRedis();
    if (tenantId == null || playerId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
    String value = UUID.randomUUID().toString();
    Ticket ticket = new Ticket(tenantId, playerId, Instant.now().plusSeconds(TICKET_TTL_SECONDS));
    try {
      Boolean stored = redis.opsForValue().setIfAbsent(
          TICKET_KEY_PREFIX + value, objectMapper.writeValueAsString(ticket),
          Duration.ofSeconds(TICKET_TTL_SECONDS));
      if (!Boolean.TRUE.equals(stored)) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实时通道票据创建失败");
      }
      return value;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("Competition player realtime ticket issue failed, tenantId={}, playerId={}", tenantId, playerId);
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
  }

  public Ticket consume(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    requireRedis();
    try {
      String json = redis.opsForValue().getAndDelete(TICKET_KEY_PREFIX + value.trim());
      if (!StringUtils.hasText(json)) {
        return null;
      }
      Ticket ticket = objectMapper.readValue(json, Ticket.class);
      if (ticket.tenantId() == null || ticket.playerId() == null
          || ticket.expiresAt() == null || ticket.expiresAt().isBefore(Instant.now())) {
        return null;
      }
      return ticket;
    } catch (JsonProcessingException ex) {
      log.warn("Competition player realtime ticket payload invalid");
      return null;
    } catch (RuntimeException ex) {
      log.warn("Competition player realtime ticket consume failed");
      return null;
    }
  }

  public int ttlSeconds() {
    return TICKET_TTL_SECONDS;
  }

  private void requireRedis() {
    if (redis == null) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
  }
}
