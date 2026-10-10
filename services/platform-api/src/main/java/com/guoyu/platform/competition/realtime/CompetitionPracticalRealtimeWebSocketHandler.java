package com.guoyu.platform.competition.realtime;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CompetitionPracticalRealtimeWebSocketHandler extends TextWebSocketHandler {
  private final CompetitionPracticalRealtimeTicketService ticketService;
  private final CompetitionPracticalRealtimePublisher publisher;

  public CompetitionPracticalRealtimeWebSocketHandler(
      CompetitionPracticalRealtimeTicketService ticketService,
      CompetitionPracticalRealtimePublisher publisher) {
    this.ticketService = ticketService;
    this.publisher = publisher;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    if (uri == null || uri.getRawQuery() == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }
    Map<String, String> query = Arrays.stream(uri.getRawQuery().split("&"))
        .map(value -> value.split("=", 2)).filter(value -> value.length == 2)
        .collect(Collectors.toMap(value -> value[0], value -> value[1], (a, b) -> b));
    var ticket = ticketService.consume(query.get("ticket"));
    Long tenantId = parseLong(query.get("tenantId"));
    boolean allowed = ticket != null && tenantId != null && tenantId.equals(ticket.tenantId())
        && (ticket.permissions().contains("competition:practical-judge:score")
            || ticket.permissions().contains("competition:practical-console:view"));
    if (!allowed) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid competition practical ticket"));
      return;
    }
    session.getAttributes().put("tenantId", tenantId);
    publisher.register(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    publisher.unregister(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    publisher.unregister(session);
    if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
  }

  private Long parseLong(String value) {
    try { return value == null ? null : Long.valueOf(value); }
    catch (NumberFormatException ex) { return null; }
  }
}
