package com.guoyu.platform.competition.player.realtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** Keeps only this API node's online competition-player homepage sessions. */
@Component
@Slf4j
public class CompetitionPlayerRealtimeHub {

  static final String ATTR_TENANT_ID = "competitionPlayerTenantId";
  static final String ATTR_PLAYER_ID = "competitionPlayerId";
  private final ConcurrentHashMap<PlayerKey, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

  public void register(WebSocketSession session, Long tenantId, Long playerId) {
    session.getAttributes().put(ATTR_TENANT_ID, tenantId);
    session.getAttributes().put(ATTR_PLAYER_ID, playerId);
    sessions.computeIfAbsent(new PlayerKey(tenantId, playerId), ignored -> ConcurrentHashMap.newKeySet())
        .add(session);
  }

  public void unregister(WebSocketSession session) {
    Long tenantId = longAttribute(session, ATTR_TENANT_ID);
    Long playerId = longAttribute(session, ATTR_PLAYER_ID);
    if (tenantId == null || playerId == null) {
      return;
    }
    PlayerKey key = new PlayerKey(tenantId, playerId);
    sessions.computeIfPresent(key, (ignored, connections) -> {
      connections.remove(session);
      return connections.isEmpty() ? null : connections;
    });
  }

  public int sendToPlayer(Long tenantId, Long playerId, String json) {
    if (tenantId == null || playerId == null || json == null) {
      return 0;
    }
    Set<WebSocketSession> connections = sessions.get(new PlayerKey(tenantId, playerId));
    if (connections == null || connections.isEmpty()) {
      return 0;
    }
    int sent = 0;
    for (WebSocketSession session : new ArrayList<>(connections)) {
      if (!session.isOpen()) {
        unregister(session);
        continue;
      }
      try {
        synchronized (session) {
          if (session.isOpen()) {
            session.sendMessage(new TextMessage(json));
            sent++;
          }
        }
      } catch (IOException | RuntimeException ex) {
        log.debug("Competition player realtime send failed, playerId={}, connectionId={}",
            playerId, session.getId());
        closeQuietly(session);
        unregister(session);
      }
    }
    return sent;
  }

  private static Long longAttribute(WebSocketSession session, String key) {
    Object value = session.getAttributes().get(key);
    if (value instanceof Long number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return value == null ? null : Long.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static void closeQuietly(WebSocketSession session) {
    try {
      if (session.isOpen()) {
        session.close(CloseStatus.SERVER_ERROR);
      }
    } catch (IOException ignored) {
      // already closing
    }
  }

  private record PlayerKey(Long tenantId, Long playerId) {
  }
}
