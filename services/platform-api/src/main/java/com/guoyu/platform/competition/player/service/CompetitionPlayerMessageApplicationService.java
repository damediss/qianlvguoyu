package com.guoyu.platform.competition.player.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.competition.messaging.vo.CompetitionPlayerMessageRow;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerMessageVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerMessageApplicationService {

  private final CompetitionPlayerMessageMapper playerMessageMapper;

  public CompetitionPlayerMessageApplicationService(CompetitionPlayerMessageMapper playerMessageMapper) {
    this.playerMessageMapper = playerMessageMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerMessageVO> page(PageQuery query) {
    CompetitionPlayerPrincipal player = currentPlayer();
    Page<CompetitionPlayerMessageRow> page = playerMessageMapper.selectPageAccessible(
        new Page<>(query.getPage(), query.getPageSize()), player.getTenantId(), player.getPlayerId());
    if (page == null) {
      log.warn("Competition player message mapper returned null page, tenantId={}, playerId={}",
          player.getTenantId(), player.getPlayerId());
      return PageResponse.empty(query.getPage(), query.getPageSize());
    }
    List<CompetitionPlayerMessageVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      page.getRecords().forEach(row -> items.add(toVO(row)));
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public long unreadCount() {
    CompetitionPlayerPrincipal player = currentPlayer();
    return playerMessageMapper.countUnreadAccessible(player.getTenantId(), player.getPlayerId());
  }

  @Transactional
  public void markRead(Long messageId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    int affected = playerMessageMapper.markReadIfAccessible(
        player.getTenantId(), player.getPlayerId(), messageId, LocalDateTime.now());
    if (affected == 0) {
      log.warn("Competition player attempted inaccessible message read, tenantId={}, playerId={}, messageId={}",
          player.getTenantId(), player.getPlayerId(), messageId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在或当前赛事资格已失效");
    }
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player message request has no authenticated player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }

  private CompetitionPlayerMessageVO toVO(CompetitionPlayerMessageRow row) {
    CompetitionPlayerMessageVO vo = new CompetitionPlayerMessageVO();
    vo.setMessageId(row.getMessageId());
    vo.setEventId(row.getEventId());
    vo.setNoticeId(row.getNoticeId());
    vo.setType(row.getType());
    vo.setTitle(row.getTitle());
    vo.setContent(row.getContent());
    vo.setPublishedAt(row.getPublishedAt());
    vo.setReceivedAt(row.getReceivedAt());
    vo.setReadAt(row.getReadAt());
    return vo;
  }
}
