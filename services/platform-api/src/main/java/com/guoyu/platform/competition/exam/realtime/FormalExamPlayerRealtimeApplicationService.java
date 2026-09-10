package com.guoyu.platform.competition.exam.realtime;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalExamAccessSessionStatus;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.exam.security.CompetitionExamSecurityContext;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessCommandMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerRealtimeTicketVO;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamPlayerRealtimeApplicationService {

  private final FormalExamWsTicketService ticketService;
  private final FormalExamRealtimeProperties properties;
  private final CompetitionFormalExamAccessSessionMapper accessSessionMapper;
  private final CompetitionFormalExamAccessCommandMapper commandMapper;
  private final Clock clock;

  public FormalExamPlayerRealtimeApplicationService(
      FormalExamWsTicketService ticketService,
      FormalExamRealtimeProperties properties,
      CompetitionFormalExamAccessSessionMapper accessSessionMapper,
      CompetitionFormalExamAccessCommandMapper commandMapper,
      Clock clock) {
    this.ticketService = ticketService;
    this.properties = properties;
    this.accessSessionMapper = accessSessionMapper;
    this.commandMapper = commandMapper;
    this.clock = clock;
  }

  public CompetitionFormalExamPlayerRealtimeTicketVO issueTicket() {
    if (!properties.isEnabled()) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
    CompetitionExamPrincipal principal = currentPlayer();
    if (principal.getAccessSessionId() == null || principal.getGeneration() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
    }
    CompetitionFormalExamAccessSessionEntity access =
        accessSessionMapper.selectById(principal.getAccessSessionId());
    if (access == null
        || !CompetitionFormalExamAccessSessionStatus.ACTIVE.getCode().equals(access.getStatus())
        || !principal.getGeneration().equals(access.getGeneration())
        || !principal.getPlayerId().equals(access.getPlayerId())
        || !principal.getTenantId().equals(access.getTenantId())) {
      log.warn("Formal exam WS ticket rejected: access session inactive or generation mismatch, playerId={}",
          principal.getPlayerId());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
    }
    Instant expiresAt = Instant.now(clock).plusSeconds(ticketService.ttlSeconds());
    String ticket = ticketService.issue(new FormalExamWsTicketService.Ticket(
        principal.getTenantId(),
        principal.getPlayerId(),
        access.getId(),
        access.getGeneration(),
        access.getExamSessionId(),
        access.getCandidateExamId(),
        expiresAt));
    return new CompetitionFormalExamPlayerRealtimeTicketVO(
        ticket,
        ticketService.ttlSeconds(),
        properties.getWsPath(),
        properties.getHeartbeatSeconds());
  }

  @Transactional
  public void ack(Long tenantId, Long accessSessionId, Integer generation, String commandIdValue) {
    if (accessSessionId == null || generation == null || !StringUtils.hasText(commandIdValue)) {
      return;
    }
    Long commandId;
    try {
      commandId = Long.valueOf(commandIdValue.trim());
    } catch (NumberFormatException ex) {
      return;
    }
    int updated = commandMapper.markAckedIfIssued(
        commandId, accessSessionId, generation, LocalDateTime.now(clock));
    if (updated != 1) {
      log.warn("Formal exam command ACK rejected, accessSessionId={}, commandId={}",
          accessSessionId, commandId);
    }
  }

  public void ackFromPlayer(String commandIdValue) {
    CompetitionExamPrincipal principal = currentPlayer();
    ack(principal.getTenantId(), principal.getAccessSessionId(), principal.getGeneration(), commandIdValue);
  }

  private CompetitionExamPrincipal currentPlayer() {
    try {
      return CompetitionExamSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Formal exam realtime API called without exam login");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
    }
  }
}
