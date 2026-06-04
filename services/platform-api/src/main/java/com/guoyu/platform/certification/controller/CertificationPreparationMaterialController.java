package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationMaterialGenerateReq;
import com.guoyu.platform.certification.dto.PreparationMaterialItemSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialQueryReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationMaterialBatchVO;
import com.guoyu.platform.certification.vo.PreparationMaterialItemVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationMaterialController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationMaterialController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/{taskId}/application-materials")
  @PreAuthorize("hasAuthority('certification.preparation.material.view')")
  public ApiResponse<PageResponse<PreparationMaterialBatchVO>> pageMaterials(
      @PathVariable Long taskId,
      @Valid @ModelAttribute PreparationMaterialQueryReq req
  ) {
    return ApiResponse.ok(service.pageMaterials(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/application-materials")
  @PreAuthorize("hasAuthority('certification.preparation.material.upload')")
  @AuditLog(module = "certification", action = "preparation-material:upload", targetType = "certification_application_material_item")
  public ApiResponse<PreparationMaterialItemVO> createMaterial(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationMaterialItemSaveReq req
  ) {
    return ApiResponse.ok(service.createApplicationMaterial(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/application-materials/generate")
  @PreAuthorize("hasAuthority('certification.preparation.material.generate')")
  @AuditLog(module = "certification", action = "preparation-material:generate", targetType = "certification_application_material_batch")
  public ApiResponse<PreparationMaterialBatchVO> generateMaterials(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationMaterialGenerateReq req
  ) {
    return ApiResponse.ok(service.generateApplicationMaterials(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/application-materials/{batchId}")
  @PreAuthorize("hasAuthority('certification.preparation.material.view')")
  public ApiResponse<PreparationMaterialBatchVO> getMaterialBatch(
      @PathVariable Long taskId,
      @PathVariable Long batchId
  ) {
    return ApiResponse.ok(service.getMaterialBatch(taskId, batchId));
  }
}
