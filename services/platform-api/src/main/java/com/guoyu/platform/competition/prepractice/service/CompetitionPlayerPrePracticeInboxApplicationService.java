package com.guoyu.platform.competition.prepractice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeDispatchBatchMapper;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPlayerPrePracticeInboxRow;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeTaskVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerPrePracticeInboxApplicationService {

  private final CompetitionPrePracticeDispatchBatchMapper batchMapper;

  public CompetitionPlayerPrePracticeInboxApplicationService(
      CompetitionPrePracticeDispatchBatchMapper batchMapper) {
    this.batchMapper = batchMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerPrePracticeTaskVO> page(PageQuery query) {
    CompetitionPlayerPrincipal player = currentPlayer();
    Page<CompetitionPlayerPrePracticeInboxRow> page = batchMapper.pagePlayerInbox(
        new Page<>(query.getPage(), query.getPageSize()), player.getTenantId(), player.getPlayerId());
    if (page == null) {
      log.warn("Pre-practice inbox mapper returned null page, tenantId={}, playerId={}",
          player.getTenantId(), player.getPlayerId());
      return PageResponse.empty(query.getPage(), query.getPageSize());
    }
    List<CompetitionPlayerPrePracticeTaskVO> items = page.getRecords() == null
        ? List.of() : page.getRecords().stream().map(this::toVO).toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Pre-practice inbox request has no authenticated competition player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }

  private CompetitionPlayerPrePracticeTaskVO toVO(CompetitionPlayerPrePracticeInboxRow row) {
    return new CompetitionPlayerPrePracticeTaskVO(
        value(row.getDeliveryId()), value(row.getBatchId()), value(row.getTaskId()),
        row.getTaskTitleSnapshot(), value(row.getEventId()), row.getEventNameSnapshot(),
        value(row.getEventWorkTypeId()), row.getEventWorkTypeNameSnapshot(),
        row.getEventWorkTypeLevelCode(), row.getEventWorkTypeLevelNameSnapshot(),
        value(row.getStageId()), row.getStageNameSnapshot(), row.getQuestionCount(),
        row.getStatus(), row.getDeliveredAt(), row.getPushedAt());
  }

  private String value(Long value) {
    return value == null ? null : value.toString();
  }
}
