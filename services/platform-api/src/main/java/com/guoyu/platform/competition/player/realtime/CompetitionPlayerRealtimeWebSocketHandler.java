package com.guoyu.platform.competition.player.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
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
public class CompetitionPlayerRealtimeWebSocketHandler extends TextWebSocketHandler {

  private final CompetitionPlayerRealtimeTicketService ticketService;
  private final CompetitionPlayerRealtimeHub hub;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerRealtimeWebSocketHandler(
      CompetitionPlayerRealtimeTicketService ticketService,
      CompetitionPlayerRealtimeHub hub,
      ObjectMapper objectMapper) {
    this.ticketService = ticketService;
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
    CompetitionPlayerRealtimeTicketService.Ticket ticket =
        ticketService.consume(decode(query.get("ticket")));
    if (ticket == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid ticket"));
      return;
    }
    hub.register(session, ticket.tenantId(), ticket.playerId());
    log.debug("Competition player realtime connected, playerId={}, connectionId={}",
        ticket.playerId(), session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    try {
      JsonNode root = objectMapper.readTree(message.getPayload());
      if (root != null && "ping".equals(root.path("type").asText())) {
        synchronized (session) {
          if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
          }
        }
      }
    } catch (IOException ex) {
      log.debug("Ignoring malformed competition player realtime heartbeat, connectionId={}", session.getId());
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
}
