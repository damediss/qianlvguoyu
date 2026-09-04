package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SubsidyMaterialExportReq;
import com.guoyu.platform.trainingdepartment.dto.SubsidyTransitionReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyClassMaterialService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyMaterialExportApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyMaterialInstanceApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyMaterialOfficeApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyPackProfileApplicationService;
import com.guoyu.platform.trainingdepartment.vo.SubsidyEventVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialInstanceListVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialInstanceVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialOfficeConfigVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyPackProfileVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSubsidyController {

  private final TrainSubsidyApplicationService trainSubsidyApplicationService;
  private final TrainSubsidyPackProfileApplicationService profileService;
  private final TrainSubsidyClassMaterialService classMaterialService;
  private final TrainSubsidyMaterialInstanceApplicationService materialInstanceService;
  private final TrainSubsidyMaterialExportApplicationService materialExportService;
  private final TrainSubsidyMaterialOfficeApplicationService materialOfficeService;

  public TrainSubsidyController(
      TrainSubsidyApplicationService trainSubsidyApplicationService,
      TrainSubsidyPackProfileApplicationService profileService,
      TrainSubsidyClassMaterialService classMaterialService,
      TrainSubsidyMaterialInstanceApplicationService materialInstanceService,
      TrainSubsidyMaterialExportApplicationService materialExportService,
      TrainSubsidyMaterialOfficeApplicationService materialOfficeService) {
    this.trainSubsidyApplicationService = trainSubsidyApplicationService;
    this.profileService = profileService;
    this.classMaterialService = classMaterialService;
    this.materialInstanceService = materialInstanceService;
    this.materialExportService = materialExportService;
    this.materialOfficeService = materialOfficeService;
  }

  @GetMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<PageResponse<TrainSubsidyVO>> page(@Valid @ModelAttribute TrainSubsidyQueryReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.page(req));
  }

  @GetMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<TrainSubsidyVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.getById(id));
  }

  /** 本班进行中补贴单（无则 data=null） */
  @GetMapping("/info-classes/{classId}/subsidy")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<TrainSubsidyVO> getByClassId(@PathVariable Long classId) {
    return ApiResponse.ok(trainSubsidyApplicationService.getActiveByClassId(classId));
  }

  /**
   * 按班幂等建单：已有进行中单则返回已有，否则创建草稿。
   */
  @PostMapping("/info-classes/{classId}/subsidy/ensure")
  @PreAuthorize("hasAuthority('train:subsidy:create')")
  @AuditLog(module = "training", action = "subsidy:ensure", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> ensureForClass(
      @PathVariable Long classId,
      @RequestBody(required = false) TrainSubsidyCreateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.ensureForClass(classId, req));
  }

  @PostMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:create')")
  @AuditLog(module = "training", action = "subsidy:create", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> create(@Valid @RequestBody TrainSubsidyCreateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.create(req));
  }

  @PutMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:update", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> update(@PathVariable Long id, @Valid @RequestBody TrainSubsidyUpdateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.update(id, req));
  }

  @DeleteMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:delete')")
  @AuditLog(module = "training", action = "subsidy:delete", targetType = "train_subsidy")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSubsidyApplicationService.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/subsidies/{id}/transition")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:transition", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> transition(
      @PathVariable Long id, @Valid @RequestBody SubsidyTransitionReq body) {
    return ApiResponse.ok(trainSubsidyApplicationService.transition(id, body));
  }

  @GetMapping("/subsidies/{id}/events")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<SubsidyEventVO>> events(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.events(id));
  }

  @GetMapping("/subsidies/{id}/slot-files")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<Map<String, Object>>> slotFiles(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.listSlotFiles(id));
  }

  @PostMapping(value = "/subsidies/{id}/slot-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:upload-slot", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> uploadSlot(
      @PathVariable Long id,
      @RequestParam String slotCode,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainSubsidyApplicationService.uploadSlotFile(id, slotCode, file));
  }

  @PostMapping("/subsidies/{id}/materials/generate")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:generate", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> generate(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.generatePack(id));
  }

  /** 本班上游材料预览（证书/照片/名册来源） */
  @GetMapping("/subsidies/{id}/class-materials")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<Map<String, Object>> classMaterialsPreview(@PathVariable Long id) {
    return ApiResponse.ok(classMaterialService.preview(id));
  }

  /** 从本班刷新：证书 PDF、获证名册写入对应槽位（保留非 [班] 前缀的人工上传） */
  @PostMapping("/subsidies/{id}/class-materials/sync")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:sync-class", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> syncClassMaterials(@PathVariable Long id) {
    Map<String, Object> result = classMaterialService.syncFromClass(id);
    materialInstanceService.refreshFromSlotFiles(id);
    return ApiResponse.ok(result);
  }

  /** 一键生成本班全体学员《个人技能提升补贴申领表（2025）》 */
  @PostMapping("/subsidies/{id}/class-materials/generate-claim-forms")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:generate-claim-forms", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> generateClaimForms(@PathVariable Long id) {
    Map<String, Object> result = classMaterialService.generatePersonalClaimForms(id);
    materialInstanceService.refreshFromSlotFiles(id);
    return ApiResponse.ok(result);
  }

  /** 材料实例清单（按类型分组） */
  @GetMapping("/subsidies/{id}/material-instances")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<SubsidyMaterialInstanceListVO> materialInstances(@PathVariable Long id) {
    return ApiResponse.ok(materialInstanceService.listInstances(id));
  }

  /** 替换材料文件（照片/证书/盖章件） */
  @PostMapping(value = "/material-instances/{instanceId}/replace-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:replace-material", targetType = "train_subsidy_material")
  public ApiResponse<SubsidyMaterialInstanceVO> replaceMaterialFile(
      @PathVariable Long instanceId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(materialInstanceService.replaceFile(instanceId, file));
  }

  /** 确认当前材料版本 */
  @PostMapping("/material-instances/{instanceId}/confirm")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:confirm-material", targetType = "train_subsidy_material")
  public ApiResponse<SubsidyMaterialInstanceVO> confirmMaterial(@PathVariable Long instanceId) {
    return ApiResponse.ok(materialInstanceService.confirmInstance(instanceId));
  }

  /** OnlyOffice 编辑器配置（document key / url / callback / JWT） */
  @GetMapping("/material-instances/{instanceId}/office-config")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  public ApiResponse<SubsidyMaterialOfficeConfigVO> officeConfig(@PathVariable Long instanceId) {
    return ApiResponse.ok(materialOfficeService.buildEditorConfig(instanceId));
  }

  /**
   * Document Server 拉取文档（带短期签名 token，免登录）。
   * 路径需加入租户白名单：/api/training/office/**
   */
  @GetMapping("/office/files/{fileId}")
  public ResponseEntity<Resource> officeFileDownload(
      @PathVariable Long fileId,
      @RequestParam("token") String token) {
    return materialOfficeService.downloadForOffice(fileId, token);
  }

  /** OnlyOffice 保存回调（免登录；返回 {"error":0}） */
  @PostMapping("/office/callback")
  public Map<String, Object> officeCallback(
      @RequestParam Long instanceId,
      @RequestParam(required = false) Long versionId,
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long userId,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody(required = false) String body) {
    return materialOfficeService.handleCallback(instanceId, versionId, tenantId, userId, authorization, body);
  }

  /** 按勾选材料导出 ZIP（按类型分目录） */
  @PostMapping("/subsidies/{id}/materials/export")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:export-selected", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> exportMaterials(
      @PathVariable Long id, @Valid @RequestBody SubsidyMaterialExportReq body) {
    return ApiResponse.ok(materialExportService.exportSelected(id, body.instanceIds()));
  }

  @DeleteMapping("/subsidies/{id}/slot-files/{fileId}")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:delete-slot", targetType = "train_subsidy")
  public ApiResponse<Void> deleteSlotFile(@PathVariable Long id, @PathVariable Long fileId) {
    trainSubsidyApplicationService.deleteSlotFile(id, fileId);
    return ApiResponse.ok();
  }

  @GetMapping("/subsidy-pack-profiles")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<SubsidyPackProfileVO>> profiles(
      @RequestParam(required = false) String regionCode,
      @RequestParam(required = false) String trainMode,
      @RequestParam(required = false) Integer policyYear,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(profileService.list(regionCode, trainMode, policyYear, status));
  }

  @GetMapping("/subsidy-pack-profiles/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<SubsidyPackProfileVO> profile(@PathVariable Long id) {
    return ApiResponse.ok(profileService.get(id));
  }

  @PostMapping("/subsidies/{id}/submit")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:submit", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.submit(id));
  }

  @PostMapping("/subsidies/{id}/approve")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:approve", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.approve(id));
  }

  @PostMapping("/subsidies/{id}/reject")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:reject", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainSubsidyApplicationService.reject(id, reason));
  }
}
