package com.guoyu.platform.file.artifact.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.security.PermissionCodes;
import com.guoyu.platform.file.artifact.dto.ContractPreviewGenerateReq;
import com.guoyu.platform.file.artifact.service.ContractPreviewGenerationService;
import com.guoyu.platform.file.artifact.vo.GeneratedArtifactEnvelopeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contract/mcp")
@PreAuthorize("hasAuthority('" + PermissionCodes.FILE_ARTIFACT_READ + "')")
public class ContractPreviewMcpController {

  private final ContractPreviewGenerationService service;

  public ContractPreviewMcpController(ContractPreviewGenerationService service) {
    this.service = service;
  }

  @PostMapping("/preview")
  @AuditLog(module = "contract", action = "contract:generate-preview", targetType = "contract")
  public ApiResponse<GeneratedArtifactEnvelopeVO> generatePreview(
      @Valid @RequestBody ContractPreviewGenerateReq request) {
    return ApiResponse.ok(service.generate(request));
  }
}
