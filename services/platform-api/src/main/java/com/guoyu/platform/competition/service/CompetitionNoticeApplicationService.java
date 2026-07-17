package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.competition.converter.CompetitionNoticeConverter;
import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionNoticeVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionNoticeApplicationService {

  private final CompetitionNoticeService noticeService;

  public CompetitionNoticeApplicationService(CompetitionNoticeService noticeService) {
    this.noticeService = noticeService;
  }

  @Transactional
  public CompetitionNoticeVO create(Long eventId, CompetitionNoticeCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    return CompetitionNoticeConverter.toVO(noticeService.create(principal, eventId, req));
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionNoticeVO> page(Long eventId, CompetitionNoticeQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    var page = noticeService.page(principal, eventId, req);
    List<CompetitionNoticeVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      page.getRecords().forEach(notice -> items.add(CompetitionNoticeConverter.toVO(notice)));
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionNoticeVO getById(Long noticeId) {
    return CompetitionNoticeConverter.toVO(noticeService.getById(currentUser(), noticeId));
  }

  @Transactional
  public CompetitionNoticeVO update(Long noticeId, CompetitionNoticeUpdateReq req) {
    return CompetitionNoticeConverter.toVO(
        noticeService.updateDraft(currentUser(), noticeId, req));
  }

  @Transactional
  public CompetitionNoticePublishResultVO publish(Long noticeId) {
    return noticeService.publish(currentUser(), noticeId);
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition notice request has no authenticated platform user");
      throw ex;
    }
  }
}
