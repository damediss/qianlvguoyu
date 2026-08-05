package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Pure state-machine rules; persistence and authorization stay in application services. */
@Service
public class CompetitionStageLifecycleDomainService {

  private static final Map<CompetitionStageStatus, Set<CompetitionStageStatus>> TRANSITIONS =
      new EnumMap<>(CompetitionStageStatus.class);

  static {
    TRANSITIONS.put(CompetitionStageStatus.DRAFT,
        EnumSet.of(CompetitionStageStatus.CONFIGURING));
    TRANSITIONS.put(CompetitionStageStatus.CONFIGURING,
        EnumSet.of(CompetitionStageStatus.READY_LOCKED));
    TRANSITIONS.put(CompetitionStageStatus.READY_LOCKED,
        EnumSet.of(CompetitionStageStatus.IN_PROGRESS));
    TRANSITIONS.put(CompetitionStageStatus.IN_PROGRESS,
        EnumSet.of(CompetitionStageStatus.CLOSED));
    TRANSITIONS.put(CompetitionStageStatus.CLOSED,
        EnumSet.of(CompetitionStageStatus.IN_PROGRESS, CompetitionStageStatus.SETTLED));
    TRANSITIONS.put(CompetitionStageStatus.SETTLED,
        EnumSet.of(CompetitionStageStatus.RESULT_PUBLISHED));
    TRANSITIONS.put(CompetitionStageStatus.RESULT_PUBLISHED, EnumSet.noneOf(CompetitionStageStatus.class));
  }

  public CompetitionStageStatus transition(String currentCode, CompetitionStageStatus target) {
    CompetitionStageStatus current = CompetitionStageStatus.fromCode(currentCode);
    if (current == null || target == null
        || !TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "赛段状态不允许从 " + currentCode + " 变更为 "
              + (target == null ? null : target.getCode()));
    }
    return target;
  }

  public void requireWritable(String statusCode) {
    CompetitionStageStatus status = CompetitionStageStatus.fromCode(statusCode);
    if (status == null || !CompetitionStageStatus.writableStatuses().contains(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段状态不允许修改结构");
    }
  }

  public void requireInteractiveWriteAllowed(String statusCode) {
    CompetitionStageStatus status = CompetitionStageStatus.fromCode(statusCode);
    if (status != CompetitionStageStatus.IN_PROGRESS) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段不允许执行交互式操作");
    }
  }
}
