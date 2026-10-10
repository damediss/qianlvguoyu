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
import com.guoyu.platform.competition.service.CompetitionNoticeAssetApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class CompetitionPlayerMessageApplicationService {

  private final CompetitionPlayerMessageMapper playerMessageMapper;
  private final CompetitionNoticeAssetApplicationService assetService;

  @Autowired
  public CompetitionPlayerMessageApplicationService(
      CompetitionPlayerMessageMapper playerMessageMapper,
      CompetitionNoticeAssetApplicationService assetService) {
    this.playerMessageMapper = playerMessageMapper;
    this.assetService = assetService;
  }

  public CompetitionPlayerMessageApplicationService(CompetitionPlayerMessageMapper playerMessageMapper) {
    this.playerMessageMapper = playerMessageMapper;
    this.assetService = null;
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
    Map<Long, List<CompetitionNoticeAssetVO>> assetsByNotice =
        assetService == null || page.getRecords() == null
            ? Map.of()
            : assetService.listByNoticeIds(
                player.getTenantId(),
                page.getRecords().stream()
                    .map(CompetitionPlayerMessageRow::getNoticeId)
                    .distinct()
                    .toList());
    if (page.getRecords() != null) {
      page.getRecords().forEach(row ->
          items.add(toVO(row, assetsByNotice.getOrDefault(row.getNoticeId(), List.of()))));
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

  public ResponseEntity<Resource> downloadAsset(Long messageId, Long assetId) {
    if (assetService == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "公告文件不存在");
    }
    return assetService.playerDownload(messageId, assetId);
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player message request has no authenticated player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }

  private CompetitionPlayerMessageVO toVO(
      CompetitionPlayerMessageRow row, List<CompetitionNoticeAssetVO> assets) {
    CompetitionPlayerMessageVO vo = new CompetitionPlayerMessageVO();
    vo.setMessageId(row.getMessageId());
    vo.setEventId(row.getEventId());
    vo.setNoticeId(row.getNoticeId());
    vo.setType(row.getType());
    vo.setTitle(row.getTitle());
    vo.setContentType(row.getContentType());
    vo.setContent(row.getContent());
    if (assets != null && !assets.isEmpty()) {
      List<CompetitionNoticeAssetVO> playerAssets = new ArrayList<>();
      for (CompetitionNoticeAssetVO asset : assets) {
        CompetitionNoticeAssetVO playerAsset = new CompetitionNoticeAssetVO();
        playerAsset.setAssetId(asset.getAssetId());
        playerAsset.setAssetType(asset.getAssetType());
        playerAsset.setOriginalName(asset.getOriginalName());
        playerAsset.setContentType(asset.getContentType());
        playerAsset.setSizeBytes(asset.getSizeBytes());
        playerAsset.setSourceUri("/api/competition/player/messages/" + row.getMessageId()
            + "/assets/" + asset.getAssetId());
        playerAssets.add(playerAsset);
      }
      vo.setAssets(playerAssets);
    }
    vo.setPublishedAt(row.getPublishedAt());
    vo.setReceivedAt(row.getReceivedAt());
    vo.setReadAt(row.getReadAt());
    return vo;
  }
}
