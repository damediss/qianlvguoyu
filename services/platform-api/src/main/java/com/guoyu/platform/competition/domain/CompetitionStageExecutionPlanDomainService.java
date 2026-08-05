package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.enums.CompetitionExecutionPlanStatus;

public class CompetitionStageExecutionPlanDomainService {
  public void requireEditable(String status) {
    if (!CompetitionExecutionPlanStatus.DRAFT.getCode().equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布或已撤回的执行方案不可直接修改");
    }
  }

  public void requirePublishable(String status) {
    if (!CompetitionExecutionPlanStatus.DRAFT.getCode().equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿执行方案可以发布");
    }
  }

  public void requireWithdrawable(String status) {
    if (!CompetitionExecutionPlanStatus.PUBLISHED.getCode().equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有已发布执行方案可以撤回");
    }
  }
}
