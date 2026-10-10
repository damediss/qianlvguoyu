package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionTimeRangeOverlap;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSessionConflictMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSessionConflictRow;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionPlayerSessionConflictDomainService {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final CompetitionPlayerSessionConflictMapper conflictMapper;

  public CompetitionPlayerSessionConflictDomainService(
      CompetitionPlayerSessionConflictMapper conflictMapper) {
    this.conflictMapper = conflictMapper;
  }

  public void assertNoOverlap(
      Long tenantId,
      Long playerId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long excludeTheoryScheduleVersionId,
      Long excludePracticalSessionId,
      Long excludeFormalExamSessionId) {
    if (playerId == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "选手身份缺失，无法校验场次冲突");
    }
    if (!CompetitionTimeRangeOverlap.complete(startAt, endAt)) {
      return;
    }
    CompetitionPlayerSessionConflictRow conflict = conflictMapper.findFirstOverlap(
        tenantId, playerId, startAt, endAt,
        excludeTheoryScheduleVersionId, excludePracticalSessionId, excludeFormalExamSessionId);
    if (conflict == null) {
      return;
    }
    log.warn("Competition player session overlap, tenantId={}, playerId={}, source={}, event={}, workType={}, session={}",
        tenantId, playerId, conflict.getSource(), conflict.getEventName(),
        conflict.getWorkTypeName(), conflict.getSessionName());
    throw new BusinessException(ErrorCode.CONFLICT, format(conflict));
  }

  private static String format(CompetitionPlayerSessionConflictRow conflict) {
    StringBuilder message = new StringBuilder("该选手在该时段已有其他考试场次安排");
    if (StringUtils.hasText(conflict.getEventName())) {
      message.append('：').append(conflict.getEventName());
    }
    if (StringUtils.hasText(conflict.getWorkTypeName())) {
      message.append(' ').append(conflict.getWorkTypeName());
    }
    if (StringUtils.hasText(conflict.getSessionName())) {
      message.append(' ').append(conflict.getSessionName());
    }
    if (conflict.getStartAt() != null && conflict.getEndAt() != null) {
      message.append(' ')
          .append(conflict.getStartAt().format(TIME))
          .append('-')
          .append(conflict.getEndAt().format(TIME));
    }
    return message.toString();
  }
}
