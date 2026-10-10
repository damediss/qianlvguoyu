package com.guoyu.platform.competition.exam.realtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class FormalExamRealtimeHub {

  public static final String ATTR_TENANT_ID = "tenantId";
  public static final String ATTR_PLAYER_ID = "playerId";
  public static final String ATTR_ACCESS_SESSION_ID = "accessSessionId";
  public static final String ATTR_GENERATION = "generation";
  public static final String ATTR_EXAM_SESSION_ID = "examSessionId";
  public static final String ATTR_CANDIDATE_EXAM_ID = "candidateExamId";
  public static final String ATTR_CONNECTION_ID = "connectionId";
  public static final String ATTR_ROLE = "role";
  public static final String ATTR_PAGE_CANDIDATE_IDS = "pageCandidateIds";
  public static final String ROLE_PLAYER = "player";
  public static final String ROLE_CONSOLE = "console";

  private final Map<String, WebSocketSession> byConnection = new ConcurrentHashMap<>();
  private final Map<String, ConcurrentLinkedQueue<String>> queues = new ConcurrentHashMap<>();
  private final AtomicLong droppedSends = new AtomicLong();
  private final FormalExamRealtimeProperties properties;
  private final FormalExamPresenceStore presenceStore;

  public FormalExamRealtimeHub(
      FormalExamRealtimeProperties properties,
      FormalExamPresenceStore presenceStore) {
    this.properties = properties;
    this.presenceStore = presenceStore;
  }

  public void register(WebSocketSession session) {
    if (session == null) {
      return;
    }
    byConnection.put(session.getId(), session);
    queues.put(session.getId(), new ConcurrentLinkedQueue<>());
  }

  public void unregister(WebSocketSession session) {
    if (session == null) {
      return;
    }
    byConnection.remove(session.getId());
    queues.remove(session.getId());
    Long accessSessionId = longAttr(session, ATTR_ACCESS_SESSION_ID);
    String connectionId = stringAttr(session, ATTR_CONNECTION_ID);
    presenceStore.remove(accessSessionId, connectionId);
  }

  public boolean enqueue(WebSocketSession session, String json) {
    if (session == null || json == null || !session.isOpen()) {
      return false;
    }
    ConcurrentLinkedQueue<String> queue = queues.computeIfAbsent(
        session.getId(), key -> new ConcurrentLinkedQueue<>());
    if (queue.size() >= Math.max(8, properties.getSendQueueLimit())) {
      droppedSends.incrementAndGet();
      log.warn("Formal exam WS slow consumer closed, connectionId={}",
          stringAttr(session, ATTR_CONNECTION_ID));
      closeQuietly(session, CloseStatus.SESSION_NOT_RELIABLE.withReason("slow consumer"));
      return false;
    }
    queue.add(json);
    flush(session);
    return true;
  }

  public int deliver(FormalExamRealtimeEnvelope envelope, String json) {
    if (envelope == null || json == null) {
      return 0;
    }
    if (FormalExamRealtimeTypes.TARGET_ACCESS.equals(envelope.target())) {
      return deliverToAccess(envelope, json) + deliverToConsoles(envelope, json);
    }
    if (FormalExamRealtimeTypes.TARGET_CONSOLE.equals(envelope.target())) {
      return deliverToConsoles(envelope, json);
    }
    return deliverToExamSession(envelope, json);
  }

  private int deliverToAccess(FormalExamRealtimeEnvelope envelope, String json) {
    int sent = 0;
    Long accessSessionId = parseLong(envelope.accessSessionId());
    Integer generation = envelope.generation();
    for (WebSocketSession session : snapshot()) {
      if (!session.isOpen()) {
        continue;
      }
      if (!Objects.equals(accessSessionId, longAttr(session, ATTR_ACCESS_SESSION_ID))) {
        continue;
      }
      Integer sessionGeneration = intAttr(session, ATTR_GENERATION);
      if (generation != null && sessionGeneration != null && !generation.equals(sessionGeneration)) {
        continue;
      }
      if (enqueue(session, json)) {
        sent++;
        closePlayerAfterKick(session, envelope);
      }
    }
    return sent;
  }

  private int deliverToConsoles(FormalExamRealtimeEnvelope envelope, String json) {
    int sent = 0;
    Long examSessionId = parseLong(envelope.examSessionId());
    Long candidateExamId = parseLong(envelope.candidateExamId());
    boolean pageScoped = FormalExamRealtimeTypes.CANDIDATE_PRESENCE_CHANGED.equals(envelope.type())
        || FormalExamRealtimeTypes.CANDIDATE_PROGRESS_CHANGED.equals(envelope.type());
    for (WebSocketSession session : snapshot()) {
      if (!session.isOpen() || !ROLE_CONSOLE.equals(stringAttr(session, ATTR_ROLE))) {
        continue;
      }
      if (!Objects.equals(examSessionId, longAttr(session, ATTR_EXAM_SESSION_ID))) {
        continue;
      }
      if (pageScoped && !acceptsPageCandidate(session, candidateExamId)) {
        continue;
      }
      if (enqueue(session, json)) {
        sent++;
      }
    }
    return sent;
  }

  private int deliverToExamSession(FormalExamRealtimeEnvelope envelope, String json) {
    int sent = 0;
    Long examSessionId = parseLong(envelope.examSessionId());
    for (WebSocketSession session : snapshot()) {
      if (!session.isOpen()) {
        continue;
      }
      if (!Objects.equals(examSessionId, longAttr(session, ATTR_EXAM_SESSION_ID))) {
        continue;
      }
      if (enqueue(session, json)) {
        sent++;
      }
    }
    return sent;
  }

  private void closePlayerAfterKick(WebSocketSession session, FormalExamRealtimeEnvelope envelope) {
    if (envelope == null
        || !FormalExamRealtimeTypes.ACCESS_SESSION_KICKED.equals(envelope.type())) {
      return;
    }
    if (ROLE_CONSOLE.equals(stringAttr(session, ATTR_ROLE))) {
      return;
    }
    closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("kicked"));
  }

  @SuppressWarnings("unchecked")
  private static boolean acceptsPageCandidate(WebSocketSession session, Long candidateExamId) {
    if (candidateExamId == null) {
      return true;
    }
    Object raw = session.getAttributes().get(ATTR_PAGE_CANDIDATE_IDS);
    if (!(raw instanceof List<?> ids) || ids.isEmpty()) {
      return true;
    }
    for (Object id : ids) {
      if (id instanceof Long number && candidateExamId.equals(number)) {
        return true;
      }
      if (candidateExamId.toString().equals(String.valueOf(id))) {
        return true;
      }
    }
    return false;
  }

  public WebSocketSession sessionByConnection(String connectionId) {
    return connectionId == null ? null : byConnection.get(connectionId);
  }

  public int openConnectionCount() {
    int open = 0;
    for (WebSocketSession session : byConnection.values()) {
      if (session != null && session.isOpen()) {
        open++;
      }
    }
    return open;
  }

  public long droppedSendCount() {
    return droppedSends.get();
  }

  private void flush(WebSocketSession session) {
    ConcurrentLinkedQueue<String> queue = queues.get(session.getId());
    if (queue == null) {
      return;
    }
    synchronized (session) {
      String next;
      while ((next = queue.poll()) != null) {
        try {
          if (!session.isOpen()) {
            return;
          }
          session.sendMessage(new TextMessage(next));
        } catch (IOException | RuntimeException ex) {
          closeQuietly(session, CloseStatus.SERVER_ERROR);
          return;
        }
      }
    }
  }

  private List<WebSocketSession> snapshot() {
    return new ArrayList<>(byConnection.values());
  }

  private static void closeQuietly(WebSocketSession session, CloseStatus status) {
    try {
      if (session.isOpen()) {
        session.close(status);
      }
    } catch (IOException ignored) {
      // already closing
    }
  }

  static Long longAttr(WebSocketSession session, String name) {
    Object value = session.getAttributes().get(name);
    if (value instanceof Long number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return parseLong(value == null ? null : String.valueOf(value));
  }

  static Integer intAttr(WebSocketSession session, String name) {
    Object value = session.getAttributes().get(name);
    if (value instanceof Integer number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return value == null ? null : Integer.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  static String stringAttr(WebSocketSession session, String name) {
    Object value = session.getAttributes().get(name);
    return value == null ? null : String.valueOf(value);
  }

  private static Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
