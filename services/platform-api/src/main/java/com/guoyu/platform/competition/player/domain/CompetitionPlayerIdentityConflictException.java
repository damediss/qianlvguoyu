package com.guoyu.platform.competition.player.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;

/** Expected identity validation conflict that callers may convert into a row-level validation error. */
public final class CompetitionPlayerIdentityConflictException extends BusinessException {

  public CompetitionPlayerIdentityConflictException(String message) {
    super(ErrorCode.CONFLICT, message);
  }
}
