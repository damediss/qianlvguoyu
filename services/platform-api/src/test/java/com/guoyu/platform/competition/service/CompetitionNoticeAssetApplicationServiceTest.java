package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionNoticeAssetEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.mapper.CompetitionNoticeAssetMapper;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

class CompetitionNoticeAssetApplicationServiceTest {

  private final CompetitionNoticeService noticeService = Mockito.mock(CompetitionNoticeService.class);
  private final CompetitionNoticeAssetMapper assetMapper =
      Mockito.mock(CompetitionNoticeAssetMapper.class);
  private final FileApplicationService fileService = Mockito.mock(FileApplicationService.class);
  private final CompetitionNoticeAssetApplicationService service =
      new CompetitionNoticeAssetApplicationService(noticeService, assetMapper, fileService);

  @BeforeEach
  void authenticateManager() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "Admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:notice:view", "competition:notice:publish"), "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void uploadsValidPdfAndReturnsControlledAssetUri() {
    when(noticeService.lockDraftForAssetMutation(any(), Mockito.eq(9101L)))
        .thenReturn(notice("pdf", "draft"));
    when(assetMapper.selectList(any())).thenReturn(List.of());
    when(fileService.uploadForTenant(Mockito.eq(1L), any(), Mockito.eq("private")))
        .thenReturn(file(5001L, "rules.pdf", "application/pdf", 12L));
    Mockito.doAnswer(invocation -> {
      CompetitionNoticeAssetEntity entity = invocation.getArgument(0);
      entity.setId(7001L);
      return 1;
    }).when(assetMapper).insert(any(CompetitionNoticeAssetEntity.class));
    MockMultipartFile upload = new MockMultipartFile(
        "file", "rules.pdf", "application/pdf", "%PDF-1.7\nbody".getBytes());

    CompetitionNoticeAssetVO result = service.upload(9101L, "pdf", upload);

    assertThat(result.getAssetId()).isEqualTo(7001L);
    assertThat(result.getAssetType()).isEqualTo("pdf");
    assertThat(result.getSourceUri()).isEqualTo("notice-asset://7001");
    verify(noticeService).lockDraftForAssetMutation(any(), Mockito.eq(9101L));
  }

  @Test
  void rejectsPdfWithWrongExtensionMimeMagicOrSize() throws Exception {
    when(noticeService.lockDraftForAssetMutation(any(), Mockito.eq(9101L)))
        .thenReturn(notice("pdf", "draft"));
    List<MultipartFile> invalidFiles = List.of(
        new MockMultipartFile("file", "rules.txt", "application/pdf", "%PDF-".getBytes()),
        new MockMultipartFile("file", "rules.pdf", "text/plain", "%PDF-".getBytes()),
        new MockMultipartFile("file", "rules.pdf", "application/pdf", "not-pdf".getBytes()),
        oversized("rules.pdf", "application/pdf", 50L * 1024 * 1024 + 1, "%PDF-".getBytes()));

    for (MultipartFile invalid : invalidFiles) {
      assertBadUpload(invalid);
    }
    verify(fileService, never()).uploadForTenant(any(), any(), any());
  }

  @Test
  void acceptsSupportedImageMagicAndRejectsInvalidMagic() {
    when(noticeService.lockDraftForAssetMutation(any(), Mockito.eq(9101L)))
        .thenReturn(notice("markdown", "draft"));
    when(assetMapper.selectList(any())).thenReturn(List.of());
    when(fileService.uploadForTenant(Mockito.eq(1L), any(), Mockito.eq("private")))
        .thenReturn(file(5001L, "diagram.png", "image/png", 8L));
    Mockito.doAnswer(invocation -> {
      ((CompetitionNoticeAssetEntity) invocation.getArgument(0)).setId(7001L);
      return 1;
    }).when(assetMapper).insert(any(CompetitionNoticeAssetEntity.class));
    byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    CompetitionNoticeAssetVO result = service.upload(
        9101L, "inline_image",
        new MockMultipartFile("file", "diagram.png", "image/png", png));

    assertThat(result.getAssetType()).isEqualTo("inline_image");
    assertThatThrownBy(() -> service.upload(
        9101L, "inline_image", new MockMultipartFile(
            "file", "fake.png", "image/png", "not-an-image".getBytes())))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("400");
  }

  @Test
  void rejectsRequestedAssetTypeMismatchAndImageSubtypeMismatch() {
    when(noticeService.lockDraftForAssetMutation(any(), Mockito.eq(9101L)))
        .thenReturn(notice("markdown", "draft"));
    byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    assertThatThrownBy(() -> service.upload(
        9101L, "pdf",
        new MockMultipartFile("file", "diagram.pdf", "application/pdf", "%PDF-".getBytes())))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("400");
    assertThatThrownBy(() -> service.upload(
        9101L, "inline_image",
        new MockMultipartFile("file", "diagram.jpg", "image/jpeg", png)))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("400");
    verify(fileService, never()).uploadForTenant(any(), any(), any());
  }

  @Test
  void rejectsAssetMutationAfterNoticeWasPublished() {
    when(noticeService.lockDraftForAssetMutation(any(), Mockito.eq(9101L)))
        .thenThrow(new BusinessException("409", "已发布"));
    MockMultipartFile upload = new MockMultipartFile(
        "file", "rules.pdf", "application/pdf", "%PDF-1.7".getBytes());

    assertThatThrownBy(() -> service.upload(9101L, "pdf", upload))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("409");
    assertThatThrownBy(() -> service.delete(9101L, 7001L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("409");
    verify(fileService, never()).uploadForTenant(any(), any(), any());
    verify(assetMapper, never()).deleteById(Mockito.anyLong());
  }

  @Test
  void playerDownloadUsesTenantPlayerMessageAndAssetScope() {
    authenticatePlayer(9001L, 1L);
    CompetitionNoticeAssetEntity asset = asset(7001L, 9101L, 5001L);
    when(assetMapper.selectPlayerAccessible(1L, 9001L, 8001L, 7001L)).thenReturn(asset);
    ResponseEntity<org.springframework.core.io.Resource> response =
        ResponseEntity.ok(new ByteArrayResource("pdf".getBytes()));
    when(fileService.buildDownloadResponse(1L, 5001L, true)).thenReturn(response);

    assertThat(service.playerDownload(8001L, 7001L)).isSameAs(response);

    verify(assetMapper).selectPlayerAccessible(1L, 9001L, 8001L, 7001L);
  }

  @Test
  void playerDownloadRejectsWrongMessageAssetTenantOrEligibilityAsNotFound() {
    authenticatePlayer(9001L, 2L);
    when(assetMapper.selectPlayerAccessible(2L, 9001L, 8002L, 7002L)).thenReturn(null);

    assertThatThrownBy(() -> service.playerDownload(8002L, 7002L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("404");

    verify(fileService, never()).buildDownloadResponse(any(), any(), any(Boolean.class));
  }

  private void assertBadUpload(MultipartFile file) {
    String assetType = file.getOriginalFilename() != null
        && file.getOriginalFilename().toLowerCase().endsWith(".pdf") ? "pdf" : "inline_image";
    assertThatThrownBy(() -> service.upload(9101L, assetType, file))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getCode())
        .isEqualTo("400");
  }

  private MultipartFile oversized(
      String name, String contentType, long size, byte[] head) throws Exception {
    MultipartFile file = Mockito.mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getOriginalFilename()).thenReturn(name);
    when(file.getContentType()).thenReturn(contentType);
    when(file.getSize()).thenReturn(size);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(head));
    return file;
  }

  private CompetitionNoticeEntity notice(String contentType, String status) {
    CompetitionNoticeEntity notice = new CompetitionNoticeEntity();
    notice.setId(9101L);
    notice.setTenantId(1L);
    notice.setEventId(1001L);
    notice.setContentType(contentType);
    notice.setStatus(status);
    notice.setDeleted(0);
    return notice;
  }

  private CompetitionNoticeAssetEntity asset(Long id, Long noticeId, Long fileId) {
    CompetitionNoticeAssetEntity asset = new CompetitionNoticeAssetEntity();
    asset.setId(id);
    asset.setTenantId(1L);
    asset.setNoticeId(noticeId);
    asset.setFileId(fileId);
    asset.setAssetType("pdf");
    asset.setDeleted(0);
    return asset;
  }

  private FileVO file(Long id, String name, String contentType, Long size) {
    return new FileVO(
        id, name, contentType, size, "private", "uploaded", LocalDateTime.now(), "");
  }

  private void authenticatePlayer(Long playerId, Long tenantId) {
    CompetitionPlayerPrincipal principal = new CompetitionPlayerPrincipal(playerId, tenantId);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
