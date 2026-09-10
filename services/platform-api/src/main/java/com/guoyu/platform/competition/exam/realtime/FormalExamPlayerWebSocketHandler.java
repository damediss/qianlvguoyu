package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalExamAccessSessionStatus;
import com.guoyu.platform.competition.exam.observability.FormalExamNodeIds;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class FormalExamPlayerWebSocketHandler extends TextWebSocketHandler implements MessageListener {

  private final FormalExamWsTicketService ticketService;
  private final FormalExamPresenceStore presenceStore;
  private final FormalExamRealtimeHub hub;
  private final FormalExamPlayerRealtimeApplicationService realtimeService;
  private final CompetitionFormalExamAccessSessionMapper accessSessionMapper;
  private final FormalExamRealtimeProperties properties;
  private final ObjectMapper objectMapper;

  public FormalExamPlayerWebSocketHandler(
      FormalExamWsTicketService ticketService,
      FormalExamPresenceStore presenceStore,
      FormalExamRealtimeHub hub,
      FormalExamPlayerRealtimeApplicationService realtimeService,
      CompetitionFormalExamAccessSessionMapper accessSessionMapper,
      FormalExamRealtimeProperties properties,
      ObjectMapper objectMapper) {
    this.ticketService = ticketService;
    this.presenceStore = presenceStore;
    this.hub = hub;
    this.realtimeService = realtimeService;
    this.accessSessionMapper = accessSessionMapper;
    this.properties = properties;
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
    FormalExamWsTicketService.Ticket ticket = ticketService.consume(decode(query.get("ticket")));
    if (ticket == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid ticket"));
      return;
    }
    Long previous = TenantContextHolder.getTenantId();
    try {
      TenantContextHolder.setTenantId(ticket.tenantId());
      CompetitionFormalExamAccessSessionEntity access =
          accessSessionMapper.selectById(ticket.accessSessionId());
      if (access == null
          || !CompetitionFormalExamAccessSessionStatus.ACTIVE.getCode().equals(access.getStatus())
          || !ticket.generation().equals(access.getGeneration())
          || !ticket.playerId().equals(access.getPlayerId())
          || !ticket.tenantId().equals(access.getTenantId())
          || !ticket.examSessionId().equals(access.getExamSessionId())) {
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("stale generation"));
        return;
      }
      String connectionId = UUID.randomUUID().toString();
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_TENANT_ID, ticket.tenantId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_PLAYER_ID, ticket.playerId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_ACCESS_SESSION_ID, ticket.accessSessionId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_GENERATION, ticket.generation());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_EXAM_SESSION_ID, ticket.examSessionId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_CANDIDATE_EXAM_ID, ticket.candidateExamId());
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_CONNECTION_ID, connectionId);
      session.getAttributes().put(FormalExamRealtimeHub.ATTR_ROLE, FormalExamRealtimeHub.ROLE_PLAYER);
      hub.register(session);
      presenceStore.put(
          ticket.accessSessionId(),
          connectionId,
          FormalExamNodeIds.current(properties.getNodeId()),
          ticket.generation(),
          ticket.tenantId(),
          ticket.examSessionId(),
          ticket.candidateExamId());
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
    Integer generation = FormalExamRealtimeHub.intAttr(session, FormalExamRealtimeHub.ATTR_GENERATION);
    Integer messageGeneration = root.hasNonNull("generation") ? root.get("generation").asInt() : generation;
    if (generation != null && messageGeneration != null && !generation.equals(messageGeneration)) {
      log.warn("Formal exam WS stale generation rejected, connectionId={}",
          FormalExamRealtimeHub.stringAttr(session, FormalExamRealtimeHub.ATTR_CONNECTION_ID));
      return;
    }
    if (FormalExamRealtimeTypes.APPLICATION_HEARTBEAT.equals(type)) {
      presenceStore.touch(FormalExamRealtimeHub.longAttr(session, FormalExamRealtimeHub.ATTR_ACCESS_SESSION_ID));
      return;
    }
    if (FormalExamRealtimeTypes.COMMAND_ACK.equals(type)) {
      realtimeService.ack(
          FormalExamRealtimeHub.longAttr(session, FormalExamRealtimeHub.ATTR_TENANT_ID),
          FormalExamRealtimeHub.longAttr(session, FormalExamRealtimeHub.ATTR_ACCESS_SESSION_ID),
          generation,
          text(root, "commandId"));
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

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String json = message == null || message.getBody() == null ? null : new String(message.getBody());
    if (!StringUtils.hasText(json)) {
      return;
    }
    try {
      FormalExamRealtimeEnvelope envelope = objectMapper.readValue(json, FormalExamRealtimeEnvelope.class);
      hub.deliver(envelope, json);
    } catch (Exception ex) {
      log.warn("Formal exam realtime bus payload ignored: {}", ex.toString());
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

  private static String text(JsonNode root, String field) {
    if (root == null || !root.hasNonNull(field)) {
      return null;
    }
    return root.get(field).asText();
  }
}
