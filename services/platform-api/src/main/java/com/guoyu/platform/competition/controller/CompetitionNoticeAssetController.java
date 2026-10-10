package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.service.CompetitionNoticeAssetApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticeAssetVO;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/competition/notices/{noticeId}/assets")
public class CompetitionNoticeAssetController {

  private final CompetitionNoticeAssetApplicationService service;

  public CompetitionNoticeAssetController(CompetitionNoticeAssetApplicationService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('competition:notice:publish')")
  @AuditLog(module = "competition", action = "notice:asset:upload", targetType = "competition_notice_asset")
  public ApiResponse<CompetitionNoticeAssetVO> upload(
      @PathVariable Long noticeId,
      @RequestParam("assetType") String assetType,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.upload(noticeId, assetType, file));
  }

  @GetMapping("/{assetId}")
  @PreAuthorize("hasAuthority('competition:notice:view')")
  public ResponseEntity<Resource> get(
      @PathVariable Long noticeId, @PathVariable Long assetId) {
    return service.managerDownload(noticeId, assetId);
  }

  @DeleteMapping("/{assetId}")
  @PreAuthorize("hasAuthority('competition:notice:publish')")
  @AuditLog(module = "competition", action = "notice:asset:delete", targetType = "competition_notice_asset")
  public ApiResponse<Void> delete(
      @PathVariable Long noticeId, @PathVariable Long assetId) {
    service.delete(noticeId, assetId);
    return ApiResponse.ok();
  }
}
