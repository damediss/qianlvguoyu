package com.guoyu.platform.certification.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class PracticalStartEventPublisher {

  private final ObjectMapper objectMapper;
  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  public PracticalStartEventPublisher(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void register(WebSocketSession session) {
    sessions.add(session);
  }

  public void unregister(WebSocketSession session) {
    sessions.remove(session);
  }

  public void publish(Long tenantId, PracticalScoringModels.StartSessionView event) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException ex) {
      log.error("Unable to serialize practical start event", ex);
      return;
    }
    for (WebSocketSession session : sessions) {
      if (!session.isOpen()
          || !tenantId.equals(session.getAttributes().get("tenantId"))
          || !event.taskId().equals(session.getAttributes().get("taskId"))
          || !event.candidateId().equals(session.getAttributes().get("candidateId"))) {
        continue;
      }
      try {
        session.sendMessage(new TextMessage(payload));
      } catch (IOException ex) {
        log.warn("Unable to send practical start event to session {}: {}", session.getId(), ex.getMessage());
      }
    }
  }

  public void publishCompleteSign(Long tenantId, Long taskId, PracticalScoringModels.CompleteSignView view) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(new CompleteSignEvent("complete-sign", view));
    } catch (JsonProcessingException ex) {
      log.error("Unable to serialize practical complete-sign event", ex);
      return;
    }
    for (WebSocketSession session : sessions) {
      if (!session.isOpen()
          || !tenantId.equals(session.getAttributes().get("tenantId"))
          || !taskId.equals(session.getAttributes().get("taskId"))) {
        continue;
      }
      try {
        session.sendMessage(new TextMessage(payload));
      } catch (IOException ex) {
        log.warn("Unable to send practical complete-sign event to session {}: {}", session.getId(), ex.getMessage());
      }
    }
  }

  public record CompleteSignEvent(String type, PracticalScoringModels.CompleteSignView completeSign) {
  }
}
