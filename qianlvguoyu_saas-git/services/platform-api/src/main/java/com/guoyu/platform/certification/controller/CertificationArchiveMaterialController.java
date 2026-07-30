package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import com.guoyu.platform.certification.vo.CertificationArchiveMaterialVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveMaterialController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveMaterialController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @GetMapping("/records/{archiveId}/materials")
  @PreAuthorize("hasAuthority('certification.archive.material.view')")
  public ApiResponse<List<CertificationArchiveMaterialVO>> listMaterials(@PathVariable Long archiveId) {
    return ApiResponse.ok(service.listMaterials(archiveId));
  }

  @PostMapping("/records/{archiveId}/materials")
  @PreAuthorize("hasAuthority('certification.archive.material.upload')")
  @AuditLog(module = "certification", action = "archive-material:upload", targetType = "certification_archive_material")
  public ApiResponse<CertificationArchiveMaterialVO> uploadMaterial(
      @PathVariable Long archiveId,
      @RequestParam String materialType,
      @RequestParam(required = false) String materialName,
      @RequestParam("file") MultipartFile file
  ) {
    return ApiResponse.ok(service.uploadMaterial(archiveId, materialType, materialName, file));
  }

  @PutMapping("/records/{archiveId}/materials/{materialId}")
  @PreAuthorize("hasAuthority('certification.archive.material.edit')")
  @AuditLog(module = "certification", action = "archive-material:replace", targetType = "certification_archive_material")
  public ApiResponse<CertificationArchiveMaterialVO> replaceMaterial(
      @PathVariable Long archiveId,
      @PathVariable Long materialId,
      @RequestParam String materialType,
      @RequestParam(required = false) String materialName,
      @RequestParam("file") MultipartFile file
  ) {
    return ApiResponse.ok(service.uploadMaterial(archiveId, materialType, materialName, file));
  }

  @DeleteMapping("/records/{archiveId}/materials/{materialId}")
  @PreAuthorize("hasAuthority('certification.archive.material.delete')")
  @AuditLog(module = "certification", action = "archive-material:delete", targetType = "certification_archive_material")
  public ApiResponse<Void> deleteMaterial(@PathVariable Long archiveId, @PathVariable Long materialId) {
    service.deleteMaterial(archiveId, materialId);
    return ApiResponse.ok();
  }
}
