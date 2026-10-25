package com.guoyu.platform.textbook.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.textbook.dto.TextbookSectionReviewReq;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentVersionEntity;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedSection;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 节审定应用服务（W5：approve/return/reopen，方案 §7.4）。
 *
 * <p>工程行锁内 requireCurrentReviewer；当前用户不得等于 submittedBy（防改派后变相自审）；
 * 章分配 writer/reviewer 同人在分配时已拦截。严格按 §3.2 状态迁移，状态不符 409 不悄悄跳过；
 * done 一律 409（唯一重开入口 C6）。approve 不自动完成工程。
 */
@Service
public class TextbookReviewApplicationService {

  private static final Logger log = LoggerFactory.getLogger(TextbookReviewApplicationService.class);

  public static final String ACTION_APPROVE = "approve";
  public static final String ACTION_RETURN = "return";
  public static final String ACTION_REOPEN = "reopen";
  private static final int COMMENT_MAX = 1000;

  private final TextbookProjectLockService lockService;
  private final TextbookAccessService accessService;
  private final TextbookSectionContentApplicationService contentService;
  private final TextbookProgressService progressService;

  public TextbookReviewApplicationService(
      TextbookProjectLockService lockService,
      TextbookAccessService accessService,
      TextbookSectionContentApplicationService contentService,
      TextbookProgressService progressService) {
    this.lockService = lockService;
    this.accessService = accessService;
    this.contentService = contentService;
    this.progressService = progressService;
  }

  @Transactional
  public TextbookSectionContentVO review(Long sectionId, TextbookSectionReviewReq req) {
    String action = req.action();
    String comment = req.comment() == null ? "" : req.comment().trim();
    if (comment.length() > COMMENT_MAX) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "意见不能超过 1000 字");
    }
    if ((ACTION_RETURN.equals(action) || ACTION_REOPEN.equals(action)) && comment.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "打回/撤销通过意见必填（1~1000 字）");
    }

    LockedSection locked = lockService.lockBySection(sectionId);
    accessService.requireCurrentReviewer(locked.project(), locked.assignment(),
        SecurityContext.currentUser());
    TextbookSectionContentEntity content = locked.content();
    if (content == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "该节尚无正文，不能审定");
    }
    Long currentUserId = SecurityContext.currentUser().getUserId();
    if (currentUserId != null && currentUserId.equals(content.getSubmittedBy())) {
      throw new BusinessException(ErrorCode.CONFLICT, "审定人不能审定自己提交的内容");
    }
    if (req.baseVersion() == null || !req.baseVersion().equals(content.getVersion())) {
      throw contentService.versionConflict(content);
    }

    String from = content.getContentStatus();
    String to;
    switch (action) {
      case ACTION_APPROVE -> {
        requireStatus(content, TextbookSectionContentApplicationService.STATUS_SUBMITTED,
            "仅待审定（submitted）状态可通过");
        to = TextbookSectionContentApplicationService.STATUS_APPROVED;
      }
      case ACTION_RETURN -> {
        requireStatus(content, TextbookSectionContentApplicationService.STATUS_SUBMITTED,
            "仅待审定（submitted）状态可打回");
        to = TextbookSectionContentApplicationService.STATUS_RETURNED;
      }
      case ACTION_REOPEN -> {
        requireStatus(content, TextbookSectionContentApplicationService.STATUS_APPROVED,
            "仅已通过（approved）可撤销通过；工程 done 需教研先重新打开");
        to = TextbookSectionContentApplicationService.STATUS_RETURNED;
      }
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 action：" + action);
    }

    int newVersion = content.getVersion() + 1;
    int updated = contentService.updateForReview(content.getId(), content.getVersion(), to,
        newVersion, currentUserId);
    if (updated == 0) {
      throw contentService.versionConflict(content);
    }
    TextbookSectionContentVersionEntity latestSubmit =
        contentService.latestSubmitSnapshot(content.getId());
    contentService.insertReviewRecord(content.getId(), sectionId,
        latestSubmit == null ? null : latestSubmit.getSubmitRevision(),
        action, from, to, comment.isEmpty() ? null : comment);
    progressService.refreshChapterSummary(locked.chapter().getId());
    log.info("[教材] 审定：sectionId={} action={} {} -> {} version={}",
        sectionId, action, from, to, newVersion);
    return contentService.buildContentVOForLocked(sectionId, locked);
  }

  private void requireStatus(TextbookSectionContentEntity content, String expected,
      String message) {
    if (!expected.equals(content.getContentStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          message + "，当前状态：" + content.getContentStatus());
    }
  }
}
