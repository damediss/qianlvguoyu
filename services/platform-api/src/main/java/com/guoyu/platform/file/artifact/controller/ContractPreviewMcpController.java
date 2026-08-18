package com.guoyu.platform.file.artifact.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.security.PermissionCodes;
import com.guoyu.platform.contract.dto.ContractSourceResolveReq;
import com.guoyu.platform.contract.service.ContractSourceResolutionService;
import com.guoyu.platform.contract.vo.ContractSourceResolveVO;
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
  private final ContractSourceResolutionService sourceResolutionService;

  public ContractPreviewMcpController(
      ContractPreviewGenerationService service,
      ContractSourceResolutionService sourceResolutionService) {
    this.service = service;
    this.sourceResolutionService = sourceResolutionService;
  }

  @PostMapping("/source/resolve")
  @AuditLog(module = "contract", action = "contract:resolve-source", targetType = "contract_source")
  public ApiResponse<ContractSourceResolveVO> resolveSource(
      @Valid @RequestBody ContractSourceResolveReq request) {
    return ApiResponse.ok(sourceResolutionService.resolve(request));
  }

  @PostMapping("/preview")
  @AuditLog(module = "contract", action = "contract:generate-preview", targetType = "contract")
  public ApiResponse<GeneratedArtifactEnvelopeVO> generatePreview(
      @Valid @RequestBody ContractPreviewGenerateReq request) {
    return ApiResponse.ok(service.generate(request));
  }
}
