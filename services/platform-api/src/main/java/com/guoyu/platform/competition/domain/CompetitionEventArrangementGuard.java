package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
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

  /**
   * Returns whether the arrangement scope that owns a stage has been confirmed.
   *
   * <p>The event-level flag is retained for legacy event-level arrangements. New competition
   * stages are scoped to an event work type and level, so the level flag is authoritative whenever
   * a level exists. The work-type flag is only used for legacy work-type arrangements without a
   * level. An event-level confirmation remains a global lock and therefore takes precedence.
   */
  public static boolean isConfirmed(
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level) {
    if (confirmed(event == null ? null : event.getStagePlanStatus())) {
      return true;
    }
    if (level != null) {
      return confirmed(level.getStagePlanStatus());
    }
    return confirmed(workType == null ? null : workType.getStagePlanStatus());
  }

  public static void requireConfirmed(
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level) {
    if (!isConfirmed(event, workType, level)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段编排尚未确认，不能开始考试");
    }
  }

  private static boolean confirmed(String status) {
    return CompetitionStagePlanStatus.CONFIRMED.getCode().equals(status);
  }
}
