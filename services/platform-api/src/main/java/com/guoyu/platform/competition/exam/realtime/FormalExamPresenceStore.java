package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamPresenceStore {

  public record Presence(
      Long accessSessionId,
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      Integer generation,
      Instant lastSeenAt) {
  }

  private final StringRedisTemplate redis;
  private final FormalExamRealtimeProperties properties;
  private final ObjectMapper objectMapper;

  public FormalExamPresenceStore(
      @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redis,
      FormalExamRealtimeProperties properties,
      ObjectMapper objectMapper) {
    this.redis = redis;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public void put(
      Long accessSessionId,
      String connectionId,
      String nodeId,
      Integer generation) {
    put(accessSessionId, connectionId, nodeId, generation, null, null, null);
  }

  public void put(
      Long accessSessionId,
      String connectionId,
      String nodeId,
      Integer generation,
      Long tenantId,
      Long examSessionId,
      Long candidateExamId) {
    if (redis == null || accessSessionId == null || !StringUtils.hasText(connectionId)) {
      return;
    }
    try {
      Instant lastSeenAt = Instant.now();
      String key = key(accessSessionId);
      Map<String, String> values = new HashMap<>();
      values.put("connectionId", connectionId);
      values.put("nodeId", nodeId == null ? "" : nodeId);
      values.put("generation", generation == null ? "" : String.valueOf(generation));
      values.put("lastSeenAt", lastSeenAt.toString());
      if (tenantId != null) {
        values.put("tenantId", String.valueOf(tenantId));
      }
      if (examSessionId != null) {
        values.put("examSessionId", String.valueOf(examSessionId));
      }
      if (candidateExamId != null) {
        values.put("candidateExamId", String.valueOf(candidateExamId));
      }
      redis.opsForHash().putAll(key, values);
      redis.expire(key, Duration.ofSeconds(Math.max(30, properties.getPresenceTtlSeconds())));
      publish(read(accessSessionId), FormalExamConnectionStates.ONLINE, lastSeenAt);
    } catch (RuntimeException ex) {
      log.warn("Formal exam presence put failed, accessSessionId={}", accessSessionId);
    }
  }

  public void touch(Long accessSessionId) {
    if (redis == null || accessSessionId == null) {
      return;
    }
    try {
      Instant lastSeenAt = Instant.now();
      String key = key(accessSessionId);
      redis.opsForHash().put(key, "lastSeenAt", lastSeenAt.toString());
      redis.expire(key, Duration.ofSeconds(Math.max(30, properties.getPresenceTtlSeconds())));
      publish(read(accessSessionId), FormalExamConnectionStates.ONLINE, lastSeenAt);
    } catch (RuntimeException ex) {
      log.warn("Formal exam presence touch failed, accessSessionId={}", accessSessionId);
    }
  }

  public void remove(Long accessSessionId, String connectionId) {
    if (redis == null || accessSessionId == null) {
      return;
    }
    try {
      Presence current = read(accessSessionId);
      String key = key(accessSessionId);
      Object stored = redis.opsForHash().get(key, "connectionId");
      if (connectionId == null || connectionId.equals(String.valueOf(stored))) {
        redis.delete(key);
        publish(current, FormalExamConnectionStates.BRIEFLY_OFFLINE, Instant.now());
      }
    } catch (RuntimeException ex) {
      log.warn("Formal exam presence remove failed, accessSessionId={}", accessSessionId);
    }
  }

  public Presence read(Long accessSessionId) {
    if (redis == null || accessSessionId == null) {
      return null;
    }
    try {
      Map<Object, Object> values = redis.opsForHash().entries(key(accessSessionId));
      if (values == null || values.isEmpty()) {
        return null;
      }
      return new Presence(
          accessSessionId,
          parseLong(values.get("tenantId")),
          parseLong(values.get("examSessionId")),
          parseLong(values.get("candidateExamId")),
          parseInt(values.get("generation")),
          parseInstant(values.get("lastSeenAt")));
    } catch (RuntimeException ex) {
      log.warn("Formal exam presence read failed, accessSessionId={}", accessSessionId);
      return null;
    }
  }

  public Map<Long, Presence> readMany(List<Long> accessSessionIds) {
    Map<Long, Presence> result = new LinkedHashMap<>();
    if (accessSessionIds == null || accessSessionIds.isEmpty()) {
      return result;
    }
    for (Long accessSessionId : accessSessionIds) {
      Presence presence = read(accessSessionId);
      if (presence != null) {
        result.put(accessSessionId, presence);
      }
    }
    return result;
  }

  private void publish(Presence presence, String connectionState, Instant lastSeenAt) {
    if (presence == null || presence.examSessionId() == null || presence.candidateExamId() == null) {
      return;
    }
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("connectionState", connectionState);
      payload.put("lastSeenAt", lastSeenAt == null ? null : lastSeenAt.toString());
      FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
          UUID.randomUUID().toString(),
          null,
          FormalExamRealtimeTypes.CANDIDATE_PRESENCE_CHANGED,
          String.valueOf(presence.candidateExamId()),
          String.valueOf(presence.examSessionId()),
          String.valueOf(presence.candidateExamId()),
          String.valueOf(presence.accessSessionId()),
          presence.generation(),
          null,
          Instant.now(),
          Instant.now().plusSeconds(60),
          FormalExamRealtimeTypes.TARGET_CONSOLE,
          payload);
      redis.convertAndSend(FormalExamRealtimeTypes.CHANNEL, objectMapper.writeValueAsString(envelope));
    } catch (Exception ex) {
      log.warn("Formal exam presence publish failed, accessSessionId={}", presence.accessSessionId());
    }
  }

  private static String key(Long accessSessionId) {
    return FormalExamRealtimeTypes.PRESENCE_KEY_PREFIX + accessSessionId;
  }

  private static Long parseLong(Object value) {
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return null;
    }
    try {
      return Long.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Integer parseInt(Object value) {
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return null;
    }
    try {
      return Integer.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Instant parseInstant(Object value) {
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return null;
    }
    try {
      return Instant.parse(String.valueOf(value));
    } catch (RuntimeException ex) {
      return null;
    }
  }
}
