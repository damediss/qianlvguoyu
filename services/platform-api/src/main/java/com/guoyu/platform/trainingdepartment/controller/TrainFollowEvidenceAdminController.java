package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidenceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidencePatchReq;
import com.guoyu.platform.trainingdepartment.dto.FollowEvidenceReviewReq;
import com.guoyu.platform.trainingdepartment.dto.FollowPhotoPackGenerateReq;
import com.guoyu.platform.trainingdepartment.service.TrainFollowEvidenceApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowEvidenceVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 管理端跟班课堂影像 / 培训照片生成。
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainFollowEvidenceAdminController {

  private final TrainFollowEvidenceApplicationService evidenceService;

  public TrainFollowEvidenceAdminController(TrainFollowEvidenceApplicationService evidenceService) {
    this.evidenceService = evidenceService;
  }

  @GetMapping("/info-classes/{classId}/follow/evidences")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowEvidenceVO>> list(
      @PathVariable Long classId,
      @RequestParam(required = false) Long scheduleId,
      @RequestParam(required = false) String bizDate,
      @RequestParam(required = false) String reviewStatus,
      @RequestParam(required = false) String mediaType) {
    return ApiResponse.ok(evidenceService.list(classId, scheduleId, bizDate, reviewStatus, mediaType));
  }

  @GetMapping("/info-classes/{classId}/follow/evidence-coverage")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> coverage(@PathVariable Long classId) {
    return ApiResponse.ok(evidenceService.coverage(classId));
  }

  @PostMapping("/info-classes/{classId}/follow/evidences")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-evidence:create", targetType = "train_follow_evidence")
  public ApiResponse<FollowEvidenceVO> create(
      @PathVariable Long classId, @Valid @RequestBody FollowEvidenceCreateReq body) {
    return ApiResponse.ok(evidenceService.createAdmin(classId, body));
  }

  @PostMapping(value = "/info-classes/{classId}/follow/evidences/upload",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-evidence:upload", targetType = "train_follow_evidence")
  public ApiResponse<FollowEvidenceVO> upload(
      @PathVariable Long classId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) Long scheduleId,
      @RequestParam(required = false) String bizDate,
      @RequestParam(required = false) String teachFormOverride,
      @RequestParam(required = false) String classHoursOverride,
      @RequestParam(required = false) String caption,
      @RequestParam(required = false, defaultValue = "photo") String mediaType,
      @RequestParam(required = false) Integer durationSeconds) {
    return ApiResponse.ok(evidenceService.createAdminMultipart(
        classId, file, scheduleId, bizDate, teachFormOverride, classHoursOverride, caption, mediaType,
        durationSeconds));
  }

  @PatchMapping("/follow-evidences/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-evidence:patch", targetType = "train_follow_evidence")
  public ApiResponse<FollowEvidenceVO> patch(
      @PathVariable Long id, @RequestBody FollowEvidencePatchReq body) {
    return ApiResponse.ok(evidenceService.patch(id, body, SecurityContext.currentUser().getUserId(), false));
  }

  @PostMapping("/follow-evidences/{id}/review")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-evidence:review", targetType = "train_follow_evidence")
  public ApiResponse<FollowEvidenceVO> review(
      @PathVariable Long id, @RequestBody FollowEvidenceReviewReq body) {
    return ApiResponse.ok(evidenceService.review(id, body, SecurityContext.currentUser().getUserId()));
  }

  @DeleteMapping("/follow-evidences/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-evidence:delete", targetType = "train_follow_evidence")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    evidenceService.delete(id, SecurityContext.currentUser().getUserId(), false);
    return ApiResponse.ok(null);
  }

  @GetMapping("/follow-evidences/{id}/preview")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ResponseEntity<byte[]> preview(@PathVariable Long id) {
    return evidenceService.previewContent(id);
  }

  @GetMapping("/info-classes/{classId}/follow/photo-pack/defaults")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> photoPackDefaults(@PathVariable Long classId) {
    return ApiResponse.ok(evidenceService.photoPackDefaults(classId));
  }

  @PostMapping("/info-classes/{classId}/follow/photo-pack/generate")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-photo-pack:generate", targetType = "train_follow_photo_pack")
  public ApiResponse<Map<String, Object>> generate(
      @PathVariable Long classId,
      @RequestBody(required = false) FollowPhotoPackGenerateReq body) {
    return ApiResponse.ok(evidenceService.generatePhotoPack(classId, body));
  }

  @GetMapping("/info-classes/{classId}/follow/photo-pack/latest")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> latest(@PathVariable Long classId) {
    return ApiResponse.ok(evidenceService.latestPhotoPack(classId));
  }
}
