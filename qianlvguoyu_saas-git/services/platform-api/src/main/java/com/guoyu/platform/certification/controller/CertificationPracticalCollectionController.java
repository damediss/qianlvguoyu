package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.collection.application.PracticalCollectionApplicationService;
import com.guoyu.platform.certification.collection.dto.CancelCurrentCollectionRequest;
import com.guoyu.platform.certification.collection.dto.CollectionExceptionQueryRequest;
import com.guoyu.platform.certification.collection.dto.BatchJoinQueueRequest;
import com.guoyu.platform.certification.collection.dto.CollectionFormExportRequest;
import com.guoyu.platform.certification.collection.dto.CollectionTaskQueryRequest;
import com.guoyu.platform.certification.collection.dto.CompleteCollectionRequest;
import com.guoyu.platform.certification.collection.dto.ExceptionReleaseRequest;
import com.guoyu.platform.certification.collection.dto.JoinQueueRequest;
import com.guoyu.platform.certification.collection.dto.MarkInfoErrorRequest;
import com.guoyu.platform.certification.collection.dto.MarkPendingRetryRequest;
import com.guoyu.platform.certification.collection.dto.MatchByTokenRequest;
import com.guoyu.platform.certification.collection.dto.RemoveQueueRequest;
import com.guoyu.platform.certification.collection.dto.ScanMatchCurrentRequest;
import com.guoyu.platform.certification.collection.dto.SearchCandidateRequest;
import com.guoyu.platform.certification.collection.dto.StartNextRequest;
import com.guoyu.platform.certification.collection.dto.SubmitFingerprintRequest;
import com.guoyu.platform.certification.collection.dto.SubmitSignatureRequest;
import com.guoyu.platform.certification.collection.vo.CandidateCollectionDetailVO;
import com.guoyu.platform.certification.collection.vo.CandidateMatchVO;
import com.guoyu.platform.certification.collection.vo.CollectionActionResultVO;
import com.guoyu.platform.certification.collection.vo.CollectionExceptionVO;
import com.guoyu.platform.certification.collection.vo.BatchJoinQueueResultVO;
import com.guoyu.platform.certification.collection.vo.CollectionFormExportVO;
import com.guoyu.platform.certification.collection.vo.CollectionTaskVO;
import com.guoyu.platform.certification.collection.vo.CollectionWorkbenchVO;
import com.guoyu.platform.certification.collection.vo.FingerprintTraceVO;
import com.guoyu.platform.certification.collection.vo.QrCodeExportVO;
import com.guoyu.platform.certification.collection.vo.QueueItemVO;
import com.guoyu.platform.certification.collection.vo.ScanMatchResultVO;
import com.guoyu.platform.certification.collection.vo.SignatureRecordVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/certification/practical-collection")
public class CertificationPracticalCollectionController {

  private final PracticalCollectionApplicationService service;

  public CertificationPracticalCollectionController(PracticalCollectionApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('certification.practical.collection.view')")
  public ApiResponse<PageResponse<CollectionTaskVO>> pageTasks(
      @Valid @ModelAttribute CollectionTaskQueryRequest request
  ) {
    return ApiResponse.ok(service.pageTasks(request));
  }

  @GetMapping("/workbench/{taskId}")
  @PreAuthorize("hasAuthority('certification.practical.collection.view')")
  public ApiResponse<CollectionWorkbenchVO> workbench(@PathVariable Long taskId) {
    return ApiResponse.ok(service.workbench(taskId));
  }

  @PostMapping("/tasks/{taskId}/initialize")
  @PreAuthorize("hasAuthority('certification.practical.collection.manage')")
  @AuditLog(module = "certification", action = "practical-collection:initialize", targetType = "certification_practical_collection_status")
  public ApiResponse<CollectionActionResultVO> initialize(@PathVariable Long taskId) {
    return ApiResponse.ok(service.initializeCollectionStatusAction(taskId));
  }

  @PostMapping("/tasks/{taskId}/qrcodes/generate")
  @PreAuthorize("hasAuthority('certification.practical.collection.manage')")
  @AuditLog(module = "certification", action = "practical-collection:qrcode-generate", targetType = "certification_practical_collection_status")
  public ApiResponse<CollectionActionResultVO> generateQrCodes(@PathVariable Long taskId) {
    return ApiResponse.ok(service.generateQrTokensAction(taskId));
  }

  @GetMapping("/tasks/{taskId}/qrcodes/export")
  @PreAuthorize("hasAuthority('certification.practical.collection.export')")
  @AuditLog(module = "certification", action = "practical-collection:qrcode-export", targetType = "certification_practical_collection_status")
  public ResponseEntity<InputStreamResource> exportQrCodes(@PathVariable Long taskId) {
    QrCodeExportVO result = service.exportQrCodes(taskId);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }

  @PostMapping("/match-by-token")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:match-by-token", targetType = "certification_practical_collection_status")
  public ApiResponse<CandidateMatchVO> matchByToken(@Valid @RequestBody MatchByTokenRequest request) {
    return ApiResponse.ok(service.matchByToken(request));
  }

  @PostMapping("/workbench/{taskId}/scan-match-current")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:scan-match-current", targetType = "certification_practical_collection_status")
  public ApiResponse<ScanMatchResultVO> scanMatchCurrent(
      @PathVariable Long taskId,
      @Valid @RequestBody ScanMatchCurrentRequest request
  ) {
    return ApiResponse.ok(service.scanMatchCurrent(taskId, request.token()));
  }

  @PostMapping("/workbench/{taskId}/candidates/{candidateId}/open")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:open-candidate", targetType = "certification_practical_collection_status")
  public ApiResponse<CandidateCollectionDetailVO> openCandidateCollection(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.openCandidateCollection(taskId, candidateId));
  }

  @PostMapping("/workbench/{taskId}/candidates/{candidateId}/cancel-current")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:cancel-current", targetType = "certification_practical_collection_status")
  public ApiResponse<CollectionActionResultVO> cancelCurrentCollection(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.cancelCurrentCollection(new CancelCurrentCollectionRequest(taskId, candidateId)));
  }

  @PostMapping("/search-candidate")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  public ApiResponse<List<CandidateMatchVO>> searchCandidate(
      @Valid @RequestBody SearchCandidateRequest request
  ) {
    return ApiResponse.ok(service.searchCandidate(request));
  }

  @PostMapping("/queue/join")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:queue-join", targetType = "certification_practical_collection_queue")
  public ApiResponse<QueueItemVO> joinQueue(@Valid @RequestBody JoinQueueRequest request) {
    return ApiResponse.ok(service.joinQueue(request));
  }

  @PostMapping("/queue/join-batch")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:queue-join-batch", targetType = "certification_practical_collection_queue")
  public ApiResponse<BatchJoinQueueResultVO> joinQueueBatch(@Valid @RequestBody BatchJoinQueueRequest request) {
    return ApiResponse.ok(service.joinQueueBatch(request));
  }

  @PostMapping("/queue/remove")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:queue-remove", targetType = "certification_practical_collection_queue")
  public ApiResponse<QueueItemVO> removeQueue(@Valid @RequestBody RemoveQueueRequest request) {
    return ApiResponse.ok(service.removeQueue(request));
  }

  @PostMapping("/queue/start-next")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:start-next", targetType = "certification_practical_collection_queue")
  public ApiResponse<CandidateCollectionDetailVO> startNext(@Valid @RequestBody StartNextRequest request) {
    return ApiResponse.ok(service.startNext(request));
  }

  @PostMapping("/fingerprint/submit")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:fingerprint-submit", targetType = "certification_practical_fingerprint_trace")
  public ApiResponse<FingerprintTraceVO> submitFingerprint(
      @Valid @RequestBody SubmitFingerprintRequest request
  ) {
    return ApiResponse.ok(service.submitFingerprint(request));
  }

  @PostMapping("/signature/submit")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:signature-submit", targetType = "certification_practical_signature_record")
  public ApiResponse<SignatureRecordVO> submitSignature(
      @Valid @RequestBody SubmitSignatureRequest request
  ) {
    return ApiResponse.ok(service.submitSignature(request));
  }

  @PostMapping("/complete")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:complete", targetType = "certification_practical_collection_status")
  public ApiResponse<CollectionActionResultVO> complete(
      @Valid @RequestBody CompleteCollectionRequest request
  ) {
    return ApiResponse.ok(service.completeCollection(request));
  }

  @PostMapping("/mark-pending-retry")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:mark-pending-retry", targetType = "certification_practical_collection_exception")
  public ApiResponse<CollectionActionResultVO> markPendingRetry(
      @Valid @RequestBody MarkPendingRetryRequest request
  ) {
    return ApiResponse.ok(service.markPendingRetry(request));
  }

  @PostMapping("/mark-info-error")
  @PreAuthorize("hasAuthority('certification.practical.collection.operate')")
  @AuditLog(module = "certification", action = "practical-collection:mark-info-error", targetType = "certification_practical_collection_exception")
  public ApiResponse<CollectionActionResultVO> markInfoError(
      @Valid @RequestBody MarkInfoErrorRequest request
  ) {
    return ApiResponse.ok(service.markInfoError(request));
  }

  @PostMapping("/exception-release")
  @PreAuthorize("hasAuthority('certification.practical.collection.exception')")
  @AuditLog(module = "certification", action = "practical-collection:exception-release", targetType = "certification_practical_collection_exception")
  public ApiResponse<CollectionActionResultVO> exceptionRelease(
      @Valid @RequestBody ExceptionReleaseRequest request
  ) {
    return ApiResponse.ok(service.exceptionRelease(request));
  }

  @GetMapping("/detail/{taskId}/{candidateId}")
  @PreAuthorize("hasAuthority('certification.practical.collection.view')")
  public ApiResponse<CandidateCollectionDetailVO> detail(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.collectionDetail(taskId, candidateId));
  }

  @GetMapping("/exceptions")
  @PreAuthorize("hasAuthority('certification.practical.collection.view')")
  public ApiResponse<PageResponse<CollectionExceptionVO>> exceptions(
      @Valid @ModelAttribute CollectionExceptionQueryRequest request
  ) {
    return ApiResponse.ok(service.pageExceptions(request));
  }

  @PostMapping("/tasks/{taskId}/collection-forms/export")
  @PreAuthorize("hasAuthority('certification.practical.collection.export')")
  @AuditLog(module = "certification", action = "practical-collection:collection-form-export", targetType = "certification_practical_collection_status")
  public ResponseEntity<InputStreamResource> exportCollectionForms(
      @PathVariable Long taskId,
      @Valid @RequestBody CollectionFormExportRequest request
  ) {
    CollectionFormExportVO result = service.exportCollectionForms(taskId, request);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }
}
