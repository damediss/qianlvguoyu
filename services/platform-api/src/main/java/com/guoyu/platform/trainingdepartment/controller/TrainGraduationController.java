package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.GraduationCertImportCommitReq;
import com.guoyu.platform.trainingdepartment.dto.GraduationStudentUpdateReq;
import com.guoyu.platform.trainingdepartment.service.GraduationCertificateImportApplicationService;
import com.guoyu.platform.trainingdepartment.service.GraduationCertifiedRosterExportService;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 结业与毕业典礼（O3）
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainGraduationController {

  private final TrainGraduationApplicationService trainGraduationApplicationService;
  private final GraduationCertificateImportApplicationService graduationCertificateImportApplicationService;
  private final GraduationCertifiedRosterExportService graduationCertifiedRosterExportService;

  public TrainGraduationController(
      TrainGraduationApplicationService trainGraduationApplicationService,
      GraduationCertificateImportApplicationService graduationCertificateImportApplicationService,
      GraduationCertifiedRosterExportService graduationCertifiedRosterExportService) {
    this.trainGraduationApplicationService = trainGraduationApplicationService;
    this.graduationCertificateImportApplicationService = graduationCertificateImportApplicationService;
    this.graduationCertifiedRosterExportService = graduationCertifiedRosterExportService;
  }

  @GetMapping("/info-classes/graduation-candidates")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> graduationCandidates(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String graduateStatus,
      @RequestParam(required = false) List<String> trainTitle,
      @RequestParam(required = false) List<String> companyName,
      @RequestParam(required = false) List<String> workTypeName) {
    return ApiResponse.ok(
        trainGraduationApplicationService.graduationCandidates(
            page, pageSize, keyword, graduateStatus, trainTitle, companyName, workTypeName));
  }

  @GetMapping("/info-classes/{classId}/graduation-students")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> graduationStudents(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationStudents(classId));
  }

  @GetMapping("/info-classes/{classId}/graduation-students/{peopleId}")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> graduationStudentDetail(
      @PathVariable Long classId, @PathVariable Long peopleId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationStudentDetail(classId, peopleId));
  }

  @PatchMapping("/info-classes/{classId}/graduation-students/{peopleId}")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "graduation-student:update", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> updateGraduationStudent(
      @PathVariable Long classId,
      @PathVariable Long peopleId,
      @RequestBody(required = false) GraduationStudentUpdateReq body) {
    return ApiResponse.ok(trainGraduationApplicationService.updateGraduationStudent(classId, peopleId, body));
  }

  @PostMapping("/graduation/certificates/import/preview")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update','train:graduation-ceremony:view')")
  @AuditLog(module = "training", action = "graduation:cert-import-preview", targetType = "train_certificate")
  public ApiResponse<Map<String, Object>> previewCertificateImport(
      @RequestParam("classIds") List<String> classIds,
      @RequestParam("file") List<MultipartFile> files) {
    return ApiResponse.ok(graduationCertificateImportApplicationService.preview(classIds, files));
  }

  /** 异步预览任务状态（大包上传时 preview 返回 jobId） */
  @GetMapping("/graduation/certificates/import/preview-jobs/{jobId}")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update','train:graduation-ceremony:view')")
  public ApiResponse<Map<String, Object>> previewCertificateImportJob(@PathVariable String jobId) {
    return ApiResponse.ok(graduationCertificateImportApplicationService.previewJobStatus(jobId));
  }

  @PostMapping("/graduation/certificates/import/commit")
  @PreAuthorize("hasAnyAuthority('train:info-class:update','train:graduation:update')")
  @AuditLog(module = "training", action = "graduation:cert-import-commit", targetType = "train_certificate")
  public ApiResponse<Map<String, Object>> commitCertificateImport(
      @RequestBody GraduationCertImportCommitReq body) {
    return ApiResponse.ok(graduationCertificateImportApplicationService.commit(body));
  }

  @GetMapping("/info-classes/{classId}/certified-roster/export")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ResponseEntity<byte[]> exportCertifiedRoster(
      @PathVariable Long classId,
      @RequestParam(required = false) String classTitle,
      @RequestParam(required = false) String scope) {
    return graduationCertifiedRosterExportService.export(classId, classTitle, scope);
  }

  @GetMapping("/graduation/certified-roster/export")
  @PreAuthorize("hasAnyAuthority('train:info-class:view','train:graduation:view','train:graduation-ceremony:view')")
  public ResponseEntity<byte[]> exportCertifiedRosters(
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) List<Long> classIds,
      @RequestParam(required = false) String classTitle,
      @RequestParam(required = false) String scope) {
    if (classIds != null && !classIds.isEmpty()) {
      return graduationCertifiedRosterExportService.exportZip(classIds, scope);
    }
    if (classId != null) {
      return graduationCertifiedRosterExportService.export(classId, classTitle, scope);
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择班级");
  }
}
