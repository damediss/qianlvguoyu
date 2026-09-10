package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;

/**
 * 赛事发布后仍允许完善工种、赛段和选手配置，直到对应编排被确认锁定。
 */
public final class CompetitionEventArrangementGuard {

  private CompetitionEventArrangementGuard() {
  }

  public static boolean isLive(CompetitionEventEntity event) {
    if (event == null) {
      return false;
    }
    String status = event.getStatus();
    return CompetitionEventStatus.DRAFT.getCode().equals(status)
        || CompetitionEventStatus.PUBLISHED.getCode().equals(status);
  }

  public static boolean isArrangementOpen(CompetitionEventEntity event) {
    return isLive(event)
        && !CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus());
  }

  public static void requireLive(CompetitionEventEntity event, String closedMessage) {
    if (event == null) {
      return;
    }
    if (!isLive(event)) {
      throw new BusinessException(ErrorCode.CONFLICT, closedMessage);
    }
  }

  public static void requireOpen(CompetitionEventEntity event, String closedMessage) {
    if (event == null) {
      return;
    }
    requireLive(event, closedMessage);
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛事的赛段编排已确认，不能继续修改");
    }
  }
}
