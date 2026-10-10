package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationArchiveCandidateQueryReq;
import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import com.guoyu.platform.certification.vo.CertificationArchiveCandidateMaterialVO;
import com.guoyu.platform.certification.vo.CertificationArchiveCandidateVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveCandidateController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveCandidateController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @GetMapping("/records/{archiveId}/candidates")
  @PreAuthorize("hasAuthority('certification.archive.candidate.view')")
  public ApiResponse<PageResponse<CertificationArchiveCandidateVO>> pageCandidates(
      @PathVariable Long archiveId,
      @Valid @ModelAttribute CertificationArchiveCandidateQueryReq req
  ) {
    return ApiResponse.ok(service.pageCandidates(archiveId, req));
  }

  @GetMapping("/records/{archiveId}/candidates/{candidateId}/materials")
  @PreAuthorize("hasAuthority('certification.archive.candidate.view')")
  public ApiResponse<List<CertificationArchiveCandidateMaterialVO>> listCandidateMaterials(
      @PathVariable Long archiveId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.listCandidateMaterials(archiveId, candidateId));
  }
}
