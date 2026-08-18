package com.guoyu.platform.certification.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class PracticalStartWebSocketHandler extends TextWebSocketHandler {

  private final PracticalStartEventPublisher publisher;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final PracticalWebSocketTicketService ticketService;

  public PracticalStartWebSocketHandler(
      PracticalStartEventPublisher publisher,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      PracticalWebSocketTicketService ticketService
  ) {
    this.publisher = publisher;
    this.assessorMapper = assessorMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.ticketService = ticketService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    if (uri == null || uri.getRawQuery() == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthenticated practical scoring channel"));
      return;
    }
    Map<String, String> query = Arrays.stream(uri.getRawQuery().split("&"))
        .map(value -> value.split("=", 2))
        .filter(value -> value.length == 2)
        .collect(Collectors.toMap(value -> value[0], value -> value[1], (left, right) -> right));
    Long tenantId = parseLong(query.get("tenantId"));
    Long taskId = parseLong(query.get("taskId"));
    Long candidateId = parseLong(query.get("candidateId"));
    PracticalWebSocketTicketService.Ticket ticket = ticketService.consume(query.get("ticket"));
    if (tenantId == null || taskId == null
        || ticket == null
        || !tenantId.equals(ticket.tenantId())
        || !canSubscribe(ticket, taskId, candidateId)) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid practical scoring channel"));
      return;
    }
    session.getAttributes().put("tenantId", tenantId);
    session.getAttributes().put("taskId", taskId);
    if (candidateId != null) {
      session.getAttributes().put("candidateId", candidateId);
    }
    publisher.register(session);
  }

  private boolean canSubscribe(
      PracticalWebSocketTicketService.Ticket ticket,
      Long taskId,
      Long candidateId
  ) {
    if (ticket.permissions().contains("certification.practical.progress")) {
      return true;
    }
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, ticket.userId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    if (assessor == null) {
      return false;
    }
    LambdaQueryWrapper<CertificationAssessorTaskEntity> query = new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
        .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
        .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId());
    if (candidateId != null) {
      query.eq(CertificationAssessorTaskEntity::getCandidateId, candidateId);
    }
    Long count = assessorTaskMapper.selectCount(query);
    return count != null && count > 0;
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    publisher.unregister(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    publisher.unregister(session);
    if (session.isOpen()) {
      session.close(CloseStatus.SERVER_ERROR);
    }
  }

  private Long parseLong(String value) {
    try {
      return value == null ? null : Long.valueOf(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
