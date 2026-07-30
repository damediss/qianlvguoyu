package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeAssetEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionNoticeAssetMapper;
import com.guoyu.platform.competition.mapper.CompetitionNoticeMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionOutboxApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionNoticeServiceTest {

  private final CompetitionNoticeMapper mapper = Mockito.mock(CompetitionNoticeMapper.class);
  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final CompetitionOutboxApplicationService outboxApplicationService =
      Mockito.mock(CompetitionOutboxApplicationService.class);
  private final CompetitionNoticeAssetMapper assetMapper =
      Mockito.mock(CompetitionNoticeAssetMapper.class);
  private final FileApplicationService fileService = Mockito.mock(FileApplicationService.class);
  private final CompetitionNoticeService service = new CompetitionNoticeService(
      mapper, eventService, userMapper, orgMapper, outboxApplicationService,
      assetMapper, fileService);

  @Test
  void createsDraftNoticeWithServerManagedStatus() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));
    CompetitionNoticeCreateReq req = new CompetitionNoticeCreateReq("  Title  ", "Content", null);

    CompetitionNoticeEntity result = service.create(principal, 1001L, req);

    assertThat(result.getTenantId()).isEqualTo(1L);
    assertThat(result.getEventId()).isEqualTo(1001L);
    assertThat(result.getTitle()).isEqualTo("Title");
    assertThat(result.getVersion()).isEqualTo(1);
    assertThat(result.getStatus()).isEqualTo("draft");
    verify(mapper).insert(Mockito.any(CompetitionNoticeEntity.class));
  }

  @Test
  void legacyCreateWithoutContentTypeDefaultsToMarkdown() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));

    CompetitionNoticeEntity result = service.create(
        principal, 1001L, new CompetitionNoticeCreateReq("Title", "Content", 1));

    assertThat(result.getContentType()).isEqualTo("markdown");
  }

  @Test
  void createsIncompleteMarkdownAndPdfDrafts() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));

    CompetitionNoticeEntity markdown = service.create(
        principal, 1001L, new CompetitionNoticeCreateReq("Markdown", "", "markdown", 1));
    CompetitionNoticeEntity pdf = service.create(
        principal, 1001L, new CompetitionNoticeCreateReq("PDF", "", "pdf", 1));

    assertThat(markdown.getContentType()).isEqualTo("markdown");
    assertThat(pdf.getContentType()).isEqualTo("pdf");
  }

  @Test
  void queriesNoticesByEventAndStatus() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));
    Page<CompetitionNoticeEntity> page = new Page<>(1, 10);
    page.setRecords(List.of(notice(9101L, 1001L, "draft")));
    page.setTotal(1);
    when(mapper.selectPage(any(), any())).thenReturn(page);
    CompetitionNoticeQueryReq req = new CompetitionNoticeQueryReq();
    req.setStatus("draft");

    Page<CompetitionNoticeEntity> result = service.page(principal, 1001L, req);

    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRecords()).hasSize(1);
  }

  @Test
  void publishesDraftNoticeAndRecordsStatusTransition() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    CompetitionNoticeEntity notice = notice(9101L, 1001L, "draft");
    when(mapper.selectForUpdate(1L, 9101L)).thenReturn(notice);
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));
    when(mapper.updateById(notice)).thenReturn(1);

    CompetitionNoticePublishResultVO result = service.publish(principal, 9101L);

    assertThat(result.getNoticeId()).isEqualTo(9101L);
    assertThat(result.getEventId()).isEqualTo(1001L);
    assertThat(result.getBeforeStatus()).isEqualTo("draft");
    assertThat(result.getAfterStatus()).isEqualTo("published");
    assertThat(notice.getStatus()).isEqualTo("published");
    assertThat(notice.getPublishedBy()).isEqualTo(1000L);
    assertThat(notice.getPublishedAt()).isNotNull();
    verify(outboxApplicationService).enqueueNoticePublished(notice, 1000L);
  }

  @Test
  void rejectsMarkdownPublishWhenBodyIsBlank() {
    CompetitionNoticeEntity notice = publishableNotice("markdown", " ");
    stubNoticeForPublish(notice);

    assertPublishRejected(notice);
  }

  @Test
  void rejectsMarkdownPublishForExternalDataAndJavascriptImages() {
    for (String uri : List.of(
        "https://example.com/a.png",
        "data:image/png;base64,AAAA",
        "javascript:alert(1)")) {
      CompetitionNoticeEntity notice = publishableNotice("markdown", "![](" + uri + ")");
      stubNoticeForPublish(notice);

      assertPublishRejected(notice);
    }
  }

  @Test
  void rejectsMarkdownRawHtmlReferenceImagesAndNonExactImageSyntax() {
    for (String content : List.of(
        "<img src=\"https://example.com/a.png\">",
        "<!-- raw html comment -->",
        "![diagram][asset]\n[asset]: notice-asset://7001",
        "![diagram](notice-asset://7001 \"title\")",
        "![diagram]( notice-asset://7001 )")) {
      CompetitionNoticeEntity notice = publishableNotice("markdown", content);
      when(assetMapper.selectList(any()))
          .thenReturn(List.of(asset(7001L, "inline_image", 5001L)));
      stubNoticeForPublish(notice);

      assertPublishRejected(notice);
    }
  }

  @Test
  void rejectsMarkdownPublishWhenReferencedAssetBelongsToAnotherNotice() {
    CompetitionNoticeEntity notice = publishableNotice(
        "markdown", "![diagram](notice-asset://7002)");
    when(assetMapper.selectList(any())).thenReturn(List.of(asset(7001L, "inline_image", 5001L)));
    stubNoticeForPublish(notice);

    assertPublishRejected(notice);
  }

  @Test
  void rejectsMarkdownPublishWhenPdfIsBound() {
    CompetitionNoticeEntity notice = publishableNotice("markdown", "Body");
    when(assetMapper.selectList(any())).thenReturn(List.of(asset(7001L, "pdf", 5001L)));
    stubNoticeForPublish(notice);

    assertPublishRejected(notice);
  }

  @Test
  void publishesMarkdownWithOwnedInlineImage() {
    CompetitionNoticeEntity notice = publishableNotice(
        "markdown", "![diagram](notice-asset://7001)");
    when(assetMapper.selectList(any()))
        .thenReturn(List.of(asset(7001L, "inline_image", 5001L)));
    stubNoticeForPublish(notice);

    CompetitionNoticePublishResultVO result = service.publish(principal(), notice.getId());

    assertThat(result.getAfterStatus()).isEqualTo("published");
    verify(fileService).getExistingFile(1L, 5001L);
  }

  @Test
  void rejectsPdfPublishWithoutExactlyOnePdfAsset() {
    CompetitionNoticeEntity missing = publishableNotice("pdf", "Description");
    when(assetMapper.selectList(any())).thenReturn(List.of());
    stubNoticeForPublish(missing);
    assertPublishRejected(missing);

    CompetitionNoticeEntity multiple = publishableNotice("pdf", "Description");
    when(assetMapper.selectList(any())).thenReturn(List.of(
        asset(7001L, "pdf", 5001L),
        asset(7002L, "pdf", 5002L)));
    stubNoticeForPublish(multiple);
    assertPublishRejected(multiple);
  }

  @Test
  void rejectsPdfPublishWithDescriptionOverLimit() {
    CompetitionNoticeEntity notice = publishableNotice("pdf", "x".repeat(2001));
    when(assetMapper.selectList(any())).thenReturn(List.of(asset(7001L, "pdf", 5001L)));
    stubNoticeForPublish(notice);

    assertPublishRejected(notice);
  }

  @Test
  void publishesPdfWithExactlyOnePdfAsset() {
    CompetitionNoticeEntity notice = publishableNotice("pdf", "Description");
    when(assetMapper.selectList(any())).thenReturn(List.of(asset(7001L, "pdf", 5001L)));
    stubNoticeForPublish(notice);

    CompetitionNoticePublishResultVO result = service.publish(principal(), notice.getId());

    assertThat(result.getAfterStatus()).isEqualTo("published");
  }

  @Test
  void repeatedPublishReturnsConflict() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    CompetitionNoticeEntity notice = notice(9101L, 1001L, "published");
    when(mapper.selectForUpdate(1L, 9101L)).thenReturn(notice);
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));

    assertThatThrownBy(() -> service.publish(principal, 9101L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("409");
    verify(mapper, never()).updateById(Mockito.any(CompetitionNoticeEntity.class));
  }

  @Test
  void concurrentPublishRaceLeavesOnlyOnePublishedResult() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    CompetitionNoticeEntity notice = notice(9101L, 1001L, "draft");
    when(mapper.selectForUpdate(1L, 9101L)).thenAnswer(invocation -> notice);
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));
    when(mapper.updateById(notice)).thenReturn(1);

    service.publish(principal, 9101L);

    assertThatThrownBy(() -> service.publish(principal, 9101L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("409");
  }

  @Test
  void publishedNoticeCannotBeModified() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    CompetitionNoticeEntity notice = notice(9101L, 1001L, "published");
    when(mapper.selectForUpdate(1L, 9101L)).thenReturn(notice);
    when(eventService.getById(1L, 1001L)).thenReturn(platformEvent(1001L));

    assertThatThrownBy(() -> service.updateDraft(
        principal, 9101L, new CompetitionNoticeUpdateReq("New", "New content", 2)))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("409");
    verify(mapper, never()).updateById(Mockito.any(CompetitionNoticeEntity.class));
  }

  @Test
  void crossTenantNoticeIsNotFound() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(mapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.getById(principal, 9901L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("404");
  }

  @Test
  void missingEventPreventsNoticeCreation() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 9901L))
        .thenThrow(new BusinessException("404", "赛事不存在"));

    assertThatThrownBy(() -> service.create(
        principal, 9901L, new CompetitionNoticeCreateReq("Title", "Content", 1)))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("404");
    verify(mapper, never()).insert(Mockito.any(CompetitionNoticeEntity.class));
  }

  @Test
  void enterpriseNoticeRequiresOwnerOrganizationScope() {
    PlatformUserPrincipal principal = principal(1000L, 1L, "enterprise", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(enterpriseEvent(1001L, 3001L));
    UserEntity user = new UserEntity();
    user.setTenantId(1L);
    user.setId(1000L);
    user.setOrgId(4001L);
    user.setDeleted(0);
    when(userMapper.selectOne(any())).thenReturn(user);

    assertThatThrownBy(() -> service.create(
        principal, 1001L, new CompetitionNoticeCreateReq("Title", "Content", 1)))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("403");
    verify(mapper, never()).insert(Mockito.any(CompetitionNoticeEntity.class));
  }

  private CompetitionEventEntity platformEvent(Long eventId) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(eventId);
    event.setTenantId(1L);
    event.setScope(CompetitionEventScope.PLATFORM.getCode());
    event.setDeleted(0);
    return event;
  }

  private CompetitionEventEntity enterpriseEvent(Long eventId, Long ownerOrgId) {
    CompetitionEventEntity event = platformEvent(eventId);
    event.setScope(CompetitionEventScope.ENTERPRISE_INTERNAL.getCode());
    event.setOwnerOrgId(ownerOrgId);
    return event;
  }

  private CompetitionNoticeEntity notice(Long id, Long eventId, String status) {
    CompetitionNoticeEntity notice = new CompetitionNoticeEntity();
    notice.setId(id);
    notice.setTenantId(1L);
    notice.setEventId(eventId);
    notice.setTitle("Title");
    notice.setContent("Content");
    notice.setVersion(1);
    notice.setStatus(status);
    notice.setDeleted(0);
    return notice;
  }

  private CompetitionNoticeEntity publishableNotice(String contentType, String content) {
    CompetitionNoticeEntity notice = notice(9101L, 1001L, "draft");
    notice.setContentType(contentType);
    notice.setContent(content);
    return notice;
  }

  private CompetitionNoticeAssetEntity asset(Long id, String assetType, Long fileId) {
    CompetitionNoticeAssetEntity asset = new CompetitionNoticeAssetEntity();
    asset.setId(id);
    asset.setTenantId(1L);
    asset.setNoticeId(9101L);
    asset.setFileId(fileId);
    asset.setAssetType(assetType);
    asset.setDeleted(0);
    return asset;
  }

  private void stubNoticeForPublish(CompetitionNoticeEntity notice) {
    when(mapper.selectForUpdate(1L, notice.getId())).thenReturn(notice);
    when(eventService.getById(1L, notice.getEventId())).thenReturn(platformEvent(notice.getEventId()));
    when(mapper.updateById(notice)).thenReturn(1);
  }

  private void assertPublishRejected(CompetitionNoticeEntity notice) {
    assertThatThrownBy(() -> service.publish(principal(), notice.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("422");
  }

  private PlatformUserPrincipal principal() {
    return principal(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
  }

  private PlatformUserPrincipal principal(
      Long userId, Long tenantId, String username, List<String> roles) {
    return new PlatformUserPrincipal(
        userId, tenantId, username, username, "business", roles,
        List.of("competition:notice:view", "competition:notice:publish"), "test@example.com");
  }
}
