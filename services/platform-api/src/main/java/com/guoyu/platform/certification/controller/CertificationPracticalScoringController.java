package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.scoring.PracticalScoringApplicationService;
import com.guoyu.platform.certification.scoring.PracticalScoringModels;
import com.guoyu.platform.certification.scoring.PracticalStartApplicationService;
import com.guoyu.platform.certification.scoring.PracticalWebSocketTicketService;
import com.guoyu.platform.certification.scoring.AssessorWorkbenchContextService;
import com.guoyu.platform.certification.scoring.PracticalCompleteSignatureApplicationService;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/certification")
public class CertificationPracticalScoringController {

  private final PracticalScoringApplicationService service;
  private final PracticalStartApplicationService startService;
  private final PracticalWebSocketTicketService ticketService;
  private final AssessorWorkbenchContextService workbenchContextService;
  private final PracticalCompleteSignatureApplicationService completeSignatureService;

  public CertificationPracticalScoringController(
      PracticalScoringApplicationService service,
      PracticalStartApplicationService startService,
      PracticalWebSocketTicketService ticketService,
      AssessorWorkbenchContextService workbenchContextService,
      PracticalCompleteSignatureApplicationService completeSignatureService
  ) {
    this.service = service;
    this.startService = startService;
    this.ticketService = ticketService;
    this.workbenchContextService = workbenchContextService;
    this.completeSignatureService = completeSignatureService;
  }

  @GetMapping("/practical-scoring/tasks/{taskId}/setup")
  @PreAuthorize("hasAuthority('certification.practical.config')")
  public ApiResponse<PracticalScoringModels.SetupView> setup(@PathVariable Long taskId) {
    return ApiResponse.ok(service.setup(taskId));
  }

  @PutMapping("/practical-scoring/tasks/{taskId}/config")
  @PreAuthorize("hasAuthority('certification.practical.config')")
  @AuditLog(module = "certification", action = "practical-scoring:config", targetType = "certification_practical_scoring_config")
  public ApiResponse<PracticalScoringModels.ConfigView> saveConfig(
      @PathVariable Long taskId,
      @Valid @RequestBody PracticalScoringModels.SaveConfigReq req
  ) {
    return ApiResponse.ok(service.saveConfig(taskId, req));
  }

  @PutMapping("/practical-scoring/papers/{paperId}/questions/{questionId}/template")
  @PreAuthorize("hasAuthority('certification.practical.config')")
  @AuditLog(module = "certification", action = "practical-scoring:template", targetType = "exam_operate_paper_detail")
  public ApiResponse<List<PracticalScoringModels.TemplateItem>> saveTemplate(
      @PathVariable Long paperId,
      @PathVariable Long questionId,
      @Valid @RequestBody List<PracticalScoringModels.TemplateItem> items
  ) {
    return ApiResponse.ok(service.saveQuestionTemplate(paperId, questionId, items));
  }

  @PutMapping("/practical-scoring/papers/{paperId}/questions/{questionId}")
  @PreAuthorize("hasAuthority('certification.practical.config')")
  @AuditLog(module = "certification", action = "practical-scoring:question-update", targetType = "exam_operate_paper_detail")
  public ApiResponse<PracticalScoringModels.PaperQuestionOption> saveQuestion(
      @PathVariable Long paperId,
      @PathVariable Long questionId,
      @Valid @RequestBody PracticalScoringModels.UpdateQuestionReq req
  ) {
    return ApiResponse.ok(service.saveQuestion(paperId, questionId, req));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/generate")
  @PreAuthorize("hasAuthority('certification.practical.generate')")
  @AuditLog(module = "certification", action = "practical-scoring:generate", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.GenerateView> generate(@PathVariable Long taskId) {
    return ApiResponse.ok(service.generate(taskId));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/candidates/{candidateId}/link")
  @PreAuthorize("hasAuthority('certification.practical.generate')")
  @AuditLog(module = "certification", action = "practical-scoring:candidate-link", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.GenerateView> linkCandidate(
      @PathVariable Long taskId,
      @PathVariable Long candidateId,
      @Valid @RequestBody PracticalScoringModels.LinkCandidateReq req
  ) {
    return ApiResponse.ok(service.linkCandidate(taskId, candidateId, req));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/candidates/batch-link")
  @PreAuthorize("hasAuthority('certification.practical.generate')")
  @AuditLog(module = "certification", action = "practical-scoring:candidate-batch-link", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.BatchGenerateView> linkCandidates(
      @PathVariable Long taskId,
      @Valid @RequestBody PracticalScoringModels.BatchLinkCandidatesReq req
  ) {
    return ApiResponse.ok(service.linkCandidates(taskId, req));
  }

  @DeleteMapping("/practical-scoring/tasks/{taskId}/candidates/{candidateId}/generated-tasks")
  @PreAuthorize("hasAuthority('certification.practical.generate')")
  @AuditLog(module = "certification", action = "practical-scoring:candidate-cancel", targetType = "certification_assessor_task")
  public ApiResponse<Void> cancelCandidateGeneratedTasks(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    service.cancelCandidateGeneratedTasks(taskId, candidateId);
    return ApiResponse.ok();
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/papers/import")
  @PreAuthorize("hasAuthority('certification.practical.config')")
  @AuditLog(module = "certification", action = "practical-scoring:paper-import", targetType = "exam_operate_paper")
  public ApiResponse<PracticalScoringModels.ImportPaperView> importPaper(
      @PathVariable Long taskId,
      @RequestParam("file") MultipartFile file
  ) {
    return ApiResponse.ok(service.importCustomPaper(taskId, file));
  }

  @GetMapping("/practical-scoring/tasks/{taskId}/progress")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  public ApiResponse<PracticalScoringModels.ProgressView> progress(@PathVariable Long taskId) {
    return ApiResponse.ok(service.progress(taskId));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/complete")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  @AuditLog(module = "certification", action = "practical-scoring:complete-task", targetType = "certification_task_assessor")
  public ApiResponse<PracticalScoringModels.CompleteTaskView> completeTask(@PathVariable Long taskId) {
    return ApiResponse.ok(service.completeTask(taskId));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/complete-sign/request")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  @AuditLog(module = "certification", action = "practical-scoring:request-complete-sign", targetType = "certification_practical_complete_sign_session")
  public ApiResponse<PracticalScoringModels.CompleteSignView> requestCompleteSign(@PathVariable Long taskId) {
    return ApiResponse.ok(completeSignatureService.requestSignatures(taskId));
  }

  @GetMapping("/practical-scoring/tasks/{taskId}/complete-sign")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  public ApiResponse<PracticalScoringModels.CompleteSignView> completeSign(@PathVariable Long taskId) {
    return ApiResponse.ok(completeSignatureService.viewForTask(taskId));
  }

  @GetMapping("/practical-scoring/tasks/{taskId}/summary/export")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  @AuditLog(module = "certification", action = "practical-scoring:summary-export", targetType = "certification_practical_candidate_result")
  public ResponseEntity<InputStreamResource> exportSummary(
      @PathVariable Long taskId,
      @RequestParam List<Long> candidateIds
  ) throws IOException {
    ExportResultDTO result = service.exportSummary(taskId, candidateIds);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }

  @PostMapping("/practical-scoring/assessor-tasks/{assessorTaskId}/return")
  @PreAuthorize("hasAuthority('certification.practical.return')")
  @AuditLog(module = "certification", action = "practical-scoring:return", targetType = "certification_assessor_task")
  public ApiResponse<Void> returnTask(
      @PathVariable Long assessorTaskId,
      @Valid @RequestBody PracticalScoringModels.ReturnReq req
  ) {
    service.returnTask(assessorTaskId, req.reason());
    return ApiResponse.ok();
  }

  @GetMapping("/practical-scoring/assessor-tasks/{assessorTaskId}")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  public ApiResponse<PracticalScoringModels.AssessorTaskDetail> assessorTaskDetail(
      @PathVariable Long assessorTaskId
  ) {
    return ApiResponse.ok(service.adminTask(assessorTaskId));
  }

  @PutMapping("/practical-scoring/assessor-tasks/{assessorTaskId}/score")
  @PreAuthorize("hasAuthority('certification.practical.return')")
  @AuditLog(module = "certification", action = "practical-scoring:adjust-score", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.AssessorTaskDetail> adjustTaskScore(
      @PathVariable Long assessorTaskId,
      @Valid @RequestBody PracticalScoringModels.AdjustScoreReq req
  ) {
    return ApiResponse.ok(service.adjustScore(assessorTaskId, req));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/candidates/{candidateId}/confirm")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  @AuditLog(module = "certification", action = "practical-start:candidate-confirm", targetType = "certification_practical_start_session")
  public ApiResponse<PracticalScoringModels.StartSessionView> confirmCandidate(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(startService.confirmCandidate(taskId, candidateId));
  }

  @PostMapping("/practical-scoring/tasks/{taskId}/candidates/{candidateId}/absent")
  @PreAuthorize("hasAuthority('certification.practical.progress')")
  @AuditLog(module = "certification", action = "practical-scoring:candidate-absent", targetType = "certification_practical_candidate_result")
  public ApiResponse<PracticalScoringModels.StartSessionView> markCandidateAbsent(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.markCandidateAbsent(taskId, candidateId));
  }

  @GetMapping("/assessor-scoring/tasks")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<List<PracticalScoringModels.AssessorTaskListItem>> myTasks(
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.ok(service.myTasks(status));
  }

  @GetMapping("/assessor-scoring/workbench-context")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<PracticalScoringModels.AssessorWorkbenchContext> workbenchContext() {
    return ApiResponse.ok(workbenchContextService.current());
  }

  @PutMapping("/assessor-scoring/workbench-context")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-scoring:switch-workbench-task", targetType = "certification_assessor_workbench_context")
  public ApiResponse<PracticalScoringModels.AssessorWorkbenchContext> switchWorkbenchTask(
      @Valid @RequestBody PracticalScoringModels.SwitchWorkbenchTaskReq req
  ) {
    return ApiResponse.ok(workbenchContextService.switchTask(req.taskId()));
  }

  @GetMapping("/assessor-scoring/exams")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<List<PracticalScoringModels.AssessorExamListItem>> myExams(
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.ok(service.myExams(status));
  }

  @GetMapping("/assessor-scoring/exams/{taskId}/candidates/{candidateId}")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<PracticalScoringModels.AssessorExamDetail> myExam(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.myExam(taskId, candidateId));
  }

  @GetMapping("/assessor-scoring/tasks/{assessorTaskId}")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<PracticalScoringModels.AssessorTaskDetail> myTask(@PathVariable Long assessorTaskId) {
    return ApiResponse.ok(service.myTask(assessorTaskId));
  }

  @PostMapping("/assessor-scoring/tasks/{assessorTaskId}/confirm-start")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-start:assessor-confirm", targetType = "certification_practical_start_session")
  public ApiResponse<PracticalScoringModels.StartSessionView> confirmStart(
      @PathVariable Long assessorTaskId
  ) {
    return ApiResponse.ok(startService.confirmAssessor(assessorTaskId));
  }

  @PostMapping("/assessor-scoring/tasks/{assessorTaskId}/start-session")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-start:leader-start", targetType = "certification_practical_start_session")
  public ApiResponse<PracticalScoringModels.StartSessionView> startSession(
      @PathVariable Long assessorTaskId
  ) {
    return ApiResponse.ok(startService.startByAssessorTask(assessorTaskId));
  }

  @PostMapping("/assessor-scoring/tasks/{assessorTaskId}/complete-session")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-start:leader-complete", targetType = "certification_practical_start_session")
  public ApiResponse<PracticalScoringModels.StartSessionView> completeSession(
      @PathVariable Long assessorTaskId
  ) {
    return ApiResponse.ok(service.completeByAssessorTask(assessorTaskId));
  }

  @PostMapping("/assessor-scoring/websocket-ticket")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<String> websocketTicket() {
    return ApiResponse.ok(ticketService.issue());
  }

  @GetMapping("/assessor-scoring/complete-sign")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  public ApiResponse<PracticalScoringModels.CompleteSignView> myCompleteSign(
      @RequestParam Long taskId
  ) {
    return ApiResponse.ok(completeSignatureService.viewForTask(taskId));
  }

  @PostMapping("/assessor-scoring/tasks/{taskId}/complete-sign")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-scoring:complete-sign", targetType = "certification_practical_complete_signature")
  public ApiResponse<PracticalScoringModels.CompleteSignView> submitCompleteSign(
      @PathVariable Long taskId,
      @Valid @RequestBody PracticalScoringModels.SubmitCompleteSignReq req
  ) {
    return ApiResponse.ok(completeSignatureService.submitMine(taskId, req));
  }

  @PutMapping("/assessor-scoring/tasks/{assessorTaskId}/draft")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-scoring:draft", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.AssessorTaskDetail> saveDraft(
      @PathVariable Long assessorTaskId,
      @Valid @RequestBody PracticalScoringModels.SaveScoreReq req
  ) {
    return ApiResponse.ok(service.saveDraft(assessorTaskId, req));
  }

  @PostMapping("/assessor-scoring/tasks/{assessorTaskId}/submit")
  @PreAuthorize("hasAuthority('certification.practical.score')")
  @AuditLog(module = "certification", action = "practical-scoring:submit", targetType = "certification_assessor_task")
  public ApiResponse<PracticalScoringModels.AssessorTaskDetail> submit(
      @PathVariable Long assessorTaskId,
      @Valid @RequestBody PracticalScoringModels.SaveScoreReq req
  ) {
    return ApiResponse.ok(service.submit(assessorTaskId, req));
  }
}
