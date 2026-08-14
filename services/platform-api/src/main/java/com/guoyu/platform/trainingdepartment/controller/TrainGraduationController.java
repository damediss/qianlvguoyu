package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.GraduationCertificateTemplateReq;
import com.guoyu.platform.trainingdepartment.dto.GraduationGraduateReq;
import com.guoyu.platform.trainingdepartment.service.TrainGraduationApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结业与毕业典礼（O3）
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainGraduationController {

  private final TrainGraduationApplicationService trainGraduationApplicationService;

  public TrainGraduationController(TrainGraduationApplicationService trainGraduationApplicationService) {
    this.trainGraduationApplicationService = trainGraduationApplicationService;
  }

  @GetMapping("/info-classes/graduation-candidates")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> graduationCandidates(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String graduateStatus) {
    return ApiResponse.ok(
        trainGraduationApplicationService.graduationCandidates(page, pageSize, keyword, graduateStatus));
  }

  @GetMapping("/info-classes/{classId}/graduation-students")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> graduationStudents(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationStudents(classId));
  }

  @PostMapping("/info-classes/{classId}/graduate")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "info-class:graduate", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> graduate(
      @PathVariable Long classId, @RequestBody(required = false) GraduationGraduateReq body) {
    return ApiResponse.ok(trainGraduationApplicationService.graduate(classId, body));
  }

  @PostMapping("/info-classes/{classId}/graduation-notify")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "info-class:graduation-notify", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> graduationNotify(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationNotify(classId));
  }

  @GetMapping("/info-classes/{classId}/graduation-notices")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<List<Map<String, Object>>> graduationNotices(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.listNotices(classId));
  }

  @PatchMapping("/info-classes/{classId}/certificate-template")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "info-class:certificate-template", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> bindCertificateTemplate(
      @PathVariable Long classId, @RequestBody GraduationCertificateTemplateReq body) {
    return ApiResponse.ok(trainGraduationApplicationService.bindCertificateTemplate(classId, body));
  }

  @PostMapping("/info-classes/{classId}/certificates/batch-issue")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "info-class:certificate-batch-issue", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> batchIssueCertificates(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.batchIssueCertificates(classId));
  }

  @PostMapping("/info-classes/{classId}/graduation-archive")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "info-class:graduation-archive", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> createArchive(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.createArchive(classId));
  }

  @GetMapping("/info-classes/{classId}/graduation-archives")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<List<Map<String, Object>>> listArchives(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.listArchives(classId));
  }

  @GetMapping("/graduation-ledger/export")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ResponseEntity<byte[]> exportLedger(@RequestParam(required = false) Long classId) {
    return trainGraduationApplicationService.exportLedger(classId);
  }
}
