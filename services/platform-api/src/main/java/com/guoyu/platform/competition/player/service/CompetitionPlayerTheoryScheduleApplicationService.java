package com.guoyu.platform.competition.player.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.service.CompetitionTheoryScheduleApplicationService;
import com.guoyu.platform.competition.vo.CompetitionPlayerTheoryScheduleVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerTheoryScheduleApplicationService {

  private final CompetitionTheoryScheduleApplicationService scheduleService;

  public CompetitionPlayerTheoryScheduleApplicationService(
      CompetitionTheoryScheduleApplicationService scheduleService) {
    this.scheduleService = scheduleService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPlayerTheoryScheduleVO> listMine() {
    CompetitionPlayerPrincipal player = currentPlayer();
    return scheduleService.currentPlayerSchedules(player.getTenantId(), player.getPlayerId());
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player theory schedule request has no authenticated player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }
}
