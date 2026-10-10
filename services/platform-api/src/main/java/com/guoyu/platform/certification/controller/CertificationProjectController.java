package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationDocumentReq;
import com.guoyu.platform.certification.dto.CertificationDocumentReviewReq;
import com.guoyu.platform.certification.dto.CertificationMemberReq;
import com.guoyu.platform.certification.dto.CertificationRiskReq;
import com.guoyu.platform.certification.dto.CertificationRiskStatusReq;
import com.guoyu.platform.certification.dto.CertificationScheduleItemReq;
import com.guoyu.platform.certification.dto.CertificationTaskCreateReq;
import com.guoyu.platform.certification.dto.CertificationTaskQueryReq;
import com.guoyu.platform.certification.dto.CertificationTaskUpdateReq;
import com.guoyu.platform.certification.dto.CertificationWbsItemReq;
import com.guoyu.platform.certification.service.CertificationProjectApplicationService;
import com.guoyu.platform.certification.vo.CertificationDocumentVO;
import com.guoyu.platform.certification.vo.CertificationDocumentWorkflowVO;
import com.guoyu.platform.certification.vo.CertificationMemberVO;
import com.guoyu.platform.certification.vo.CertificationPhaseVO;
import com.guoyu.platform.certification.vo.CertificationRiskVO;
import com.guoyu.platform.certification.vo.CertificationScheduleItemVO;
import com.guoyu.platform.certification.vo.CertificationTaskVO;
import com.guoyu.platform.certification.vo.CertificationWbsItemVO;
import com.guoyu.platform.certification.vo.CertificationWorkbenchVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/projects")
public class CertificationProjectController {

  private final CertificationProjectApplicationService service;

  public CertificationProjectController(CertificationProjectApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('certification.project.task.list')")
  public ApiResponse<PageResponse<CertificationTaskVO>> pageTasks(
      @Valid @ModelAttribute CertificationTaskQueryReq req
  ) {
    return ApiResponse.ok(service.pageTasks(req));
  }

  @GetMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('certification.project.task.list')")
  public ApiResponse<CertificationTaskVO> getTask(@PathVariable Long taskId) {
    return ApiResponse.ok(service.getTask(taskId));
  }

  @PostMapping("/tasks")
  @PreAuthorize("hasAuthority('certification.project.task.create')")
  @AuditLog(module = "certification", action = "project-task:create", targetType = "certification_task")
  public ApiResponse<CertificationTaskVO> createTask(@Valid @RequestBody CertificationTaskCreateReq req) {
    return ApiResponse.ok(service.createTask(req));
  }

  @PutMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('certification.project.task.create')")
  @AuditLog(module = "certification", action = "project-task:update", targetType = "certification_task")
  public ApiResponse<CertificationTaskVO> updateTask(
      @PathVariable Long taskId,
      @Valid @RequestBody CertificationTaskUpdateReq req
  ) {
    return ApiResponse.ok(service.updateTask(taskId, req));
  }

  @DeleteMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('certification.project.task.delete')")
  @AuditLog(module = "certification", action = "project-task:delete", targetType = "certification_task")
  public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
    service.deleteTask(taskId);
    return ApiResponse.ok();
  }

  @GetMapping("/tasks/{taskId}/flow")
  @PreAuthorize("hasAuthority('certification.project.task.flow')")
  public ApiResponse<List<CertificationPhaseVO>> flow(@PathVariable Long taskId) {
    return ApiResponse.ok(service.flow(taskId));
  }

  @GetMapping("/tasks/{taskId}/workbench")
  @PreAuthorize("hasAuthority('certification.project.task.list')")
  public ApiResponse<CertificationWorkbenchVO> workbench(@PathVariable Long taskId) {
    return ApiResponse.ok(service.workbench(taskId));
  }

  @GetMapping("/tasks/{taskId}/documents")
  @PreAuthorize("hasAuthority('certification.project.charter.view')")
  public ApiResponse<List<CertificationDocumentVO>> listDocuments(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listDocuments(taskId));
  }

  @GetMapping("/tasks/{taskId}/documents/{documentType}")
  @PreAuthorize("hasAuthority('certification.project.charter.view')")
  public ApiResponse<CertificationDocumentVO> getDocument(
      @PathVariable Long taskId,
      @PathVariable String documentType
  ) {
    return ApiResponse.ok(service.getDocument(taskId, documentType));
  }

  @PutMapping("/tasks/{taskId}/documents/{documentType}")
  @PreAuthorize("hasAuthority('certification.project.charter.edit')")
  @AuditLog(module = "certification", action = "project-document:save", targetType = "certification_task_document")
  public ApiResponse<CertificationDocumentVO> saveDocument(
      @PathVariable Long taskId,
      @PathVariable String documentType,
      @Valid @RequestBody CertificationDocumentReq req
  ) {
    return ApiResponse.ok(service.saveDocument(taskId, documentType, req));
  }

  @PostMapping("/tasks/{taskId}/documents/{documentType}/submit")
  @PreAuthorize("hasAuthority('certification.project.document.submit')")
  @AuditLog(module = "certification", action = "project-document:submit", targetType = "certification_task_document")
  public ApiResponse<CertificationDocumentVO> submitDocument(
      @PathVariable Long taskId,
      @PathVariable String documentType
  ) {
    return ApiResponse.ok(service.submitDocument(taskId, documentType));
  }

  @PostMapping("/tasks/{taskId}/documents/{documentType}/initial-review")
  @PreAuthorize("hasAuthority('certification.project.document.initialReview')")
  @AuditLog(module = "certification", action = "project-document:initial-review", targetType = "certification_task_document")
  public ApiResponse<CertificationDocumentVO> initialReviewDocument(
      @PathVariable Long taskId,
      @PathVariable String documentType,
      @Valid @RequestBody CertificationDocumentReviewReq req
  ) {
    return ApiResponse.ok(service.initialReviewDocument(taskId, documentType, req));
  }

  @PostMapping("/tasks/{taskId}/documents/{documentType}/final-review")
  @PreAuthorize("hasAuthority('certification.project.document.finalReview')")
  @AuditLog(module = "certification", action = "project-document:final-review", targetType = "certification_task_document")
  public ApiResponse<CertificationDocumentVO> finalReviewDocument(
      @PathVariable Long taskId,
      @PathVariable String documentType,
      @Valid @RequestBody CertificationDocumentReviewReq req
  ) {
    return ApiResponse.ok(service.finalReviewDocument(taskId, documentType, req));
  }

  @GetMapping("/tasks/{taskId}/documents/{documentType}/workflow")
  @PreAuthorize("hasAuthority('certification.project.document.workflowView')")
  public ApiResponse<CertificationDocumentWorkflowVO> getDocumentWorkflow(
      @PathVariable Long taskId,
      @PathVariable String documentType
  ) {
    return ApiResponse.ok(service.getDocumentWorkflow(taskId, documentType));
  }

  @GetMapping("/tasks/{taskId}/members")
  @PreAuthorize("hasAuthority('certification.project.members.view')")
  public ApiResponse<List<CertificationMemberVO>> listMembers(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listMembers(taskId));
  }

  @GetMapping("/member-users")
  @PreAuthorize("hasAuthority('certification.project.members.edit')")
  public ApiResponse<List<com.guoyu.platform.certification.vo.CertificationMemberUserVO>> listAssignableMemberUsers() {
    return ApiResponse.ok(service.listAssignableMemberUsers());
  }

  @PutMapping("/tasks/{taskId}/members")
  @PreAuthorize("hasAuthority('certification.project.members.edit')")
  @AuditLog(module = "certification", action = "project-members:save", targetType = "certification_task_member")
  public ApiResponse<List<CertificationMemberVO>> saveMembers(
      @PathVariable Long taskId,
      @Valid @RequestBody List<CertificationMemberReq> reqs
  ) {
    return ApiResponse.ok(service.saveMembers(taskId, reqs));
  }

  @GetMapping("/tasks/{taskId}/wbs")
  @PreAuthorize("hasAuthority('certification.project.wbs.view')")
  public ApiResponse<List<CertificationWbsItemVO>> listWbs(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listWbsItems(taskId));
  }

  @PutMapping("/tasks/{taskId}/wbs")
  @PreAuthorize("hasAuthority('certification.project.wbs.edit')")
  @AuditLog(module = "certification", action = "project-wbs:save", targetType = "certification_wbs_item")
  public ApiResponse<List<CertificationWbsItemVO>> saveWbs(
      @PathVariable Long taskId,
      @Valid @RequestBody List<CertificationWbsItemReq> reqs
  ) {
    return ApiResponse.ok(service.saveWbsItems(taskId, reqs));
  }

  @GetMapping("/tasks/{taskId}/schedule")
  @PreAuthorize("hasAuthority('certification.project.schedule.view')")
  public ApiResponse<List<CertificationScheduleItemVO>> listSchedule(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listScheduleItems(taskId));
  }

  @PutMapping("/tasks/{taskId}/schedule")
  @PreAuthorize("hasAuthority('certification.project.schedule.edit')")
  @AuditLog(module = "certification", action = "project-schedule:save", targetType = "certification_schedule_item")
  public ApiResponse<List<CertificationScheduleItemVO>> saveSchedule(
      @PathVariable Long taskId,
      @Valid @RequestBody List<CertificationScheduleItemReq> reqs
  ) {
    return ApiResponse.ok(service.saveScheduleItems(taskId, reqs));
  }

  @GetMapping("/tasks/{taskId}/risks")
  @PreAuthorize("hasAuthority('certification.project.risk.view')")
  public ApiResponse<List<CertificationRiskVO>> listRisks(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listRisks(taskId));
  }

  @PutMapping("/tasks/{taskId}/risks")
  @PreAuthorize("hasAuthority('certification.project.risk.edit')")
  @AuditLog(module = "certification", action = "project-risk:save", targetType = "certification_risk")
  public ApiResponse<List<CertificationRiskVO>> saveRisk(
      @PathVariable Long taskId,
      @Valid @RequestBody List<CertificationRiskReq> reqs
  ) {
    return ApiResponse.ok(service.saveRisks(taskId, reqs));
  }

  @PutMapping("/tasks/{taskId}/risks/{riskId}/status")
  @PreAuthorize("hasAuthority('certification.project.risk.edit')")
  @AuditLog(module = "certification", action = "project-risk:status", targetType = "certification_risk")
  public ApiResponse<CertificationRiskVO> toggleRiskStatus(
      @PathVariable Long taskId,
      @PathVariable Long riskId,
      @Valid @RequestBody CertificationRiskStatusReq req
  ) {
    return ApiResponse.ok(service.toggleRiskStatus(taskId, riskId, req));
  }
}
