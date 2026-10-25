package com.guoyu.platform.textbook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.textbook.dto.TextbookSectionReviewReq;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentVersionEntity;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedSection;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 节审定（方案 §12.1-TextbookReviewApplicationServiceTest）：
 * approve/return/reopen 状态迁移、意见必填、自审拒绝、重复审定 409、done 下 reopen 409。
 */
class TextbookReviewApplicationServiceTest {

  private static final long TENANT = 10159L;
  private static final long WRITER = 880_101L;
  private static final long REVIEWER = 880_202L;
  private static final long SECTION = 9101L;

  private TextbookProjectLockService lockService;
  private TextbookAccessService accessService;
  private TextbookSectionContentApplicationService contentService;
  private TextbookProgressService progressService;
  private TextbookReviewApplicationService service;

  private TextbookProjectEntity project;
  private TextbookChapterAssignmentEntity assignment;
  private TextbookSectionContentEntity content;

  @BeforeEach
  void setUp() {
    lockService = mock(TextbookProjectLockService.class);
    accessService = mock(TextbookAccessService.class);
    contentService = mock(TextbookSectionContentApplicationService.class);
    progressService = mock(TextbookProgressService.class);
    service = new TextbookReviewApplicationService(lockService, accessService, contentService,
        progressService);

    project = new TextbookProjectEntity();
    project.setId(1L);
    project.setTenantId(TENANT);
    project.setStatus(TextbookProjectApplicationService.STATUS_WRITING);
    assignment = new TextbookChapterAssignmentEntity();
    assignment.setChapterId(9001L);
    assignment.setWriterId(WRITER);
    assignment.setReviewerId(REVIEWER);
    assignment.setActive(1);
    content = new TextbookSectionContentEntity();
    content.setId(5001L);
    content.setSectionId(SECTION);
    content.setContentStatus(TextbookSectionContentApplicationService.STATUS_SUBMITTED);
    content.setVersion(3);
    content.setSubmittedBy(WRITER);

    when(lockService.lockBySection(anyLong())).thenAnswer(
        inv -> new LockedSection(project, chapter(), section(), assignment, content));
    when(contentService.versionConflict(any())).thenAnswer(
        inv -> new BusinessException(ErrorCode.CONFLICT, "内容版本冲突",
            java.util.Map.of("reason", "VERSION_CONFLICT")));
    when(contentService.updateForReview(anyLong(), anyInt(), any(), anyInt(), anyLong()))
        .thenReturn(1);
    when(contentService.buildContentVOForLocked(anyLong(), any(LockedSection.class)))
        .thenReturn(mock(TextbookSectionContentVO.class));
    when(contentService.latestSubmitSnapshot(anyLong())).thenReturn(submitSnapshot());

    PlatformUserPrincipal principal = reviewerPrincipal();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static PlatformUserPrincipal reviewerPrincipal() {
    return new PlatformUserPrincipal(REVIEWER, TENANT, "reviewer1", "审定一", "admin",
        List.of("TEXTBOOK_REVIEWER"),
        List.of("textbook:review:view", "textbook:content:review"), null);
  }

  private static TextbookOutlineNodeEntity chapter() {
    TextbookOutlineNodeEntity chapter = new TextbookOutlineNodeEntity();
    chapter.setId(9001L);
    chapter.setProjectId(1L);
    chapter.setParentId(0L);
    chapter.setNodeLevel(2);
    return chapter;
  }

  private static TextbookOutlineNodeEntity section() {
    TextbookOutlineNodeEntity section = new TextbookOutlineNodeEntity();
    section.setId(SECTION);
    section.setProjectId(1L);
    section.setParentId(9001L);
    section.setNodeLevel(3);
    return section;
  }

  private static TextbookSectionContentVersionEntity submitSnapshot() {
    TextbookSectionContentVersionEntity snapshot = new TextbookSectionContentVersionEntity();
    snapshot.setContentId(5001L);
    snapshot.setSnapshotType(TextbookSectionContentApplicationService.SNAPSHOT_SUBMIT);
    snapshot.setSubmitRevision(1);
    return snapshot;
  }

  @Test
  @DisplayName("approve：submitted → approved，comment 可选，记录写入")
  void approveTransitions() {
    service.review(SECTION, new TextbookSectionReviewReq("approve", null, 3));
    verify(contentService).updateForReview(eq(5001L), eq(3),
        eq(TextbookSectionContentApplicationService.STATUS_APPROVED), eq(4), eq(REVIEWER));
    ArgumentCaptor<String> action = ArgumentCaptor.forClass(String.class);
    verify(contentService).insertReviewRecord(eq(5001L), eq(SECTION), eq(1),
        action.capture(), eq(TextbookSectionContentApplicationService.STATUS_SUBMITTED),
        eq(TextbookSectionContentApplicationService.STATUS_APPROVED), any());
    assertEquals("approve", action.getValue());
  }

  @Test
  @DisplayName("return：submitted → returned，意见必填 1~1000 字")
  void returnTransitionsAndCommentRequired() {
    service.review(SECTION, new TextbookSectionReviewReq("return", "内容深度不足", 3));
    verify(contentService).updateForReview(eq(5001L), eq(3),
        eq(TextbookSectionContentApplicationService.STATUS_RETURNED), eq(4), eq(REVIEWER));

    BusinessException noComment = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("return", "  ", 3)));
    assertEquals(ErrorCode.BAD_REQUEST, noComment.getCode());
    assertTrue(noComment.getMessage().contains("必填"));
  }

  @Test
  @DisplayName("reopen：仅 approved 可撤销、意见必填；工程 done 下 409，C6 后才允许")
  void reopenGuards() {
    content.setContentStatus(TextbookSectionContentApplicationService.STATUS_APPROVED);
    service.review(SECTION, new TextbookSectionReviewReq("reopen", "撤销后需再修订", 3));
    verify(contentService).updateForReview(eq(5001L), eq(3),
        eq(TextbookSectionContentApplicationService.STATUS_RETURNED), eq(4), eq(REVIEWER));

    // done 工程：requireCurrentReviewer 抛 409（唯一重开入口 C6）
    project.setStatus(TextbookProjectApplicationService.STATUS_DONE);
    doThrow(new BusinessException(ErrorCode.CONFLICT, "工程已完成（done），需教研先重新打开为 writing"))
        .when(accessService).requireCurrentReviewer(any(), any(), any());
    BusinessException doneEx = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("reopen", "意见", 3)));
    assertEquals(ErrorCode.CONFLICT, doneEx.getCode());
  }

  @Test
  @DisplayName("submittedBy == 当前审定人 → 自审拒绝")
  void selfReviewRejected() {
    content.setSubmittedBy(REVIEWER);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("approve", null, 3)));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
    assertTrue(ex.getMessage().contains("自己提交"));
  }

  @Test
  @DisplayName("重复提交/重复审定 → 状态不符 409（不悄悄跳过）")
  void wrongStatusRejected() {
    content.setContentStatus(TextbookSectionContentApplicationService.STATUS_DRAFT);
    BusinessException draft = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("approve", null, 1)));
    assertEquals(ErrorCode.CONFLICT, draft.getCode());

    content.setContentStatus(TextbookSectionContentApplicationService.STATUS_APPROVED);
    BusinessException repeat = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("approve", null, 3)));
    assertEquals(ErrorCode.CONFLICT, repeat.getCode());
  }

  @Test
  @DisplayName("baseVersion 不符 → 409 VERSION_CONFLICT")
  void baseVersionMismatchRejected() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.review(SECTION, new TextbookSectionReviewReq("approve", null, 2)));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
    assertEquals("VERSION_CONFLICT",
        ((java.util.Map<?, ?>) ex.getData()).get("reason"));
  }
}
