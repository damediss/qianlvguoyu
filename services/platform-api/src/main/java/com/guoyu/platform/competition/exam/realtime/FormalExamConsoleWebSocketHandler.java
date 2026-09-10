package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class FormalExamConsoleWebSocketHandler extends TextWebSocketHandler {

  private final FormalExamConsoleRealtimeApplicationService realtimeService;
  private final FormalExamRealtimeHub hub;
  private final ObjectMapper objectMapper;

  public FormalExamConsoleWebSocketHandler(
      FormalExamConsoleRealtimeApplicationService realtimeService,
      FormalExamRealtimeHub hub,
      ObjectMapper objectMapper) {
    this.realtimeService = realtimeService;
    this.hub = hub;
    this.objectMapper = objectMapper;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    if (uri == null || uri.getRawQuery() == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing ticket"));
      return;
    }
    Map<String, String> query = Arrays.stream(uri.getRawQuery().split("&"))
        .map(value -> value.split("=", 2))
        .filter(value -> value.length == 2)
        .collect(Collectors.toMap(value -> value[0], value -> value[1], (left, right) -> right));
    Long previous = TenantContextHolder.getTenantId();
    try {
      FormalExamConsoleRealtimeApplicationService.BoundConsole bound =
          realtimeService.bind(decode(query.get("ticket")));
      if (bound == null) {
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid ticket"));
        return;
      }
      TenantContextHolder.setTenantId(bound.tenantId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_TENANT_ID, bound.tenantId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_PLAYER_ID, bound.userId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_EXAM_SESSION_ID, bound.examSessionId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_CONNECTION_ID, UUID.randomUUID().toString());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_ROLE, FormalExamRealtimeHub.ROLE_CONSOLE);
      hub.register(session);
    } catch (RuntimeException ex) {
      log.warn("Formal exam console WS handshake rejected");
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid ticket"));
    } finally {
      if (previous == null) {
        TenantContextHolder.clear();
      } else {
        TenantContextHolder.setTenantId(previous);
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    JsonNode root;
    try {
      root = objectMapper.readTree(message.getPayload());
    } catch (IOException ex) {
      return;
    }
    String type = text(root, "type");
    if (FormalExamRealtimeTypes.APPLICATION_HEARTBEAT.equals(type)) {
      return;
    }
    if (FormalExamRealtimeTypes.CONSOLE_SUBSCRIBE_PAGE.equals(type)) {
      session.getAttributes().put(
          FormalExamRealtimeHub.ATTR_PAGE_CANDIDATE_IDS, readIds(root.get("candidateExamIds")));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    hub.unregister(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    hub.unregister(session);
    if (session.isOpen()) {
      session.close(CloseStatus.SERVER_ERROR);
    }
  }

  private static List<Long> readIds(JsonNode node) {
    List<Long> ids = new ArrayList<>();
    if (node == null || !node.isArray()) {
      return ids;
    }
    for (JsonNode item : node) {
      if (item == null || item.isNull()) {
        continue;
      }
      try {
        ids.add(Long.valueOf(item.asText()));
      } catch (NumberFormatException ignored) {
        // ignore malformed ids
      }
    }
    return ids;
  }

  private static String decode(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return value;
    }
  }

  private static String text(JsonNode root, String field) {
    if (root == null || !root.hasNonNull(field)) {
      return null;
    }
    return root.get(field).asText();
  }
}
