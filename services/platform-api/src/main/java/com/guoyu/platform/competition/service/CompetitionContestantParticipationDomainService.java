package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionTimeRangeOverlap;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionContestantParticipationDomainService {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final CompetitionRegistrationMapper registrationMapper;

  public CompetitionContestantParticipationDomainService(
      CompetitionRegistrationMapper registrationMapper) {
    this.registrationMapper = registrationMapper;
  }

  public void assertCanEnroll(
      Long tenantId,
      CompetitionEventEntity event,
      Long playerId,
      Long eventWorkTypeId) {
    if (event == null || event.getId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    if (playerId == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "名单缺少选手身份，请按最新模板重新导入");
    }
    if (registrationMapper.existsEffectiveByPlayerAndWorkType(
        tenantId, event.getId(), playerId, eventWorkTypeId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "该选手已报名当前赛项");
    }
    assertEventTimeNotOverlapping(tenantId, event, playerId);
  }

  public void assertEventTimeNotOverlapping(
      Long tenantId, CompetitionEventEntity event, Long playerId) {
    List<CompetitionEventEntity> otherEvents =
        registrationMapper.selectOtherEffectiveEventsByPlayer(tenantId, playerId, event.getId());
    if (otherEvents == null || otherEvents.isEmpty()) {
      return;
    }
    boolean currentComplete = CompetitionTimeRangeOverlap.complete(event.getStartAt(), event.getEndAt());
    for (CompetitionEventEntity other : otherEvents) {
      if (other == null) {
        continue;
      }
      boolean otherComplete = CompetitionTimeRangeOverlap.complete(other.getStartAt(), other.getEndAt());
      if (!currentComplete || !otherComplete) {
        log.warn("Competition event time missing while player already enrolled elsewhere, tenantId={}, playerId={}, eventId={}, otherEventId={}",
            tenantId, playerId, event.getId(), other.getId());
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "当前选手已参加其他赛事，但赛事时间不完整，无法校验时间冲突");
      }
      if (CompetitionTimeRangeOverlap.overlaps(
          event.getStartAt(), event.getEndAt(), other.getStartAt(), other.getEndAt())) {
        log.warn("Competition event time overlap, tenantId={}, playerId={}, eventId={}, otherEventId={}",
            tenantId, playerId, event.getId(), other.getId());
        throw new BusinessException(ErrorCode.CONFLICT, formatOverlap(other));
      }
    }
  }

  private static String formatOverlap(CompetitionEventEntity other) {
    String otherName = StringUtils.hasText(other.getName()) ? other.getName() : String.valueOf(other.getId());
    return "选手已参加赛事《" + otherName + "》（"
        + other.getStartAt().format(TIME) + "-" + other.getEndAt().format(TIME)
        + "），与当前赛事时间重叠，不能同时报名";
  }
}
