package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationAssessorDispatchReq;
import com.guoyu.platform.certification.dto.PreparationAssessorGroupSaveReq;
import com.guoyu.platform.certification.dto.PreparationAssessorQueryReq;
import com.guoyu.platform.certification.dto.PreparationAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskAssessorSaveReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationAssessorGroupVO;
import com.guoyu.platform.certification.vo.PreparationAssessorTaskVO;
import com.guoyu.platform.certification.vo.PreparationAssessorVO;
import com.guoyu.platform.certification.vo.PreparationTaskAssessorVO;
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
@RequestMapping("/certification/preparation")
public class CertificationPreparationAssessorController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationAssessorController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/assessors")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.view')")
  public ApiResponse<PageResponse<PreparationAssessorVO>> pageAssessors(@Valid @ModelAttribute PreparationAssessorQueryReq req) {
    return ApiResponse.ok(service.pageAssessors(req));
  }

  @PostMapping("/assessors")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.edit')")
  @AuditLog(module = "certification", action = "preparation-assessor:create", targetType = "certification_assessor")
  public ApiResponse<PreparationAssessorVO> createAssessor(@Valid @RequestBody PreparationAssessorSaveReq req) {
    return ApiResponse.ok(service.createAssessor(req));
  }

  @PutMapping("/assessors/{assessorId}")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.edit')")
  @AuditLog(module = "certification", action = "preparation-assessor:update", targetType = "certification_assessor")
  public ApiResponse<PreparationAssessorVO> updateAssessor(
      @PathVariable Long assessorId,
      @Valid @RequestBody PreparationAssessorSaveReq req
  ) {
    return ApiResponse.ok(service.updateAssessor(assessorId, req));
  }

  @PostMapping("/tasks/{taskId}/assessor-groups")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.assign')")
  @AuditLog(module = "certification", action = "preparation-assessor-group:create", targetType = "certification_task_assessor_group")
  public ApiResponse<PreparationAssessorGroupVO> createGroup(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationAssessorGroupSaveReq req
  ) {
    return ApiResponse.ok(service.createGroup(taskId, req));
  }

  @PutMapping("/tasks/{taskId}/assessor-groups/{groupId}")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.assign')")
  @AuditLog(module = "certification", action = "preparation-assessor-group:update", targetType = "certification_task_assessor_group")
  public ApiResponse<PreparationAssessorGroupVO> updateGroup(
      @PathVariable Long taskId,
      @PathVariable Long groupId,
      @Valid @RequestBody PreparationAssessorGroupSaveReq req
  ) {
    return ApiResponse.ok(service.updateGroup(taskId, groupId, req));
  }

  @DeleteMapping("/tasks/{taskId}/assessor-groups/{groupId}")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.assign')")
  @AuditLog(module = "certification", action = "preparation-assessor-group:delete", targetType = "certification_task_assessor_group")
  public ApiResponse<Void> deleteGroup(
      @PathVariable Long taskId,
      @PathVariable Long groupId
  ) {
    service.deleteGroup(taskId, groupId);
    return ApiResponse.ok();
  }

  @GetMapping("/tasks/{taskId}/assessor-groups")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.view')")
  public ApiResponse<List<PreparationAssessorGroupVO>> listGroups(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listGroups(taskId));
  }

  @GetMapping("/tasks/{taskId}/assessor-groups/{groupId}/members")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.view')")
  public ApiResponse<List<PreparationTaskAssessorVO>> listGroupMembers(
      @PathVariable Long taskId,
      @PathVariable Long groupId
  ) {
    return ApiResponse.ok(service.listGroupMembers(taskId, groupId));
  }

  @PostMapping("/tasks/{taskId}/assessor-groups/{groupId}/members")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.assign')")
  @AuditLog(module = "certification", action = "preparation-assessor-group:add-member", targetType = "certification_task_assessor")
  public ApiResponse<PreparationTaskAssessorVO> addGroupMember(
      @PathVariable Long taskId,
      @PathVariable Long groupId,
      @Valid @RequestBody PreparationTaskAssessorSaveReq req
  ) {
    return ApiResponse.ok(service.addGroupMember(taskId, groupId, req));
  }

  @DeleteMapping("/tasks/{taskId}/assessor-groups/{groupId}/members/{memberId}")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.assign')")
  @AuditLog(module = "certification", action = "preparation-assessor-group:remove-member", targetType = "certification_task_assessor")
  public ApiResponse<Void> removeGroupMember(
      @PathVariable Long taskId,
      @PathVariable Long groupId,
      @PathVariable Long memberId
  ) {
    service.removeGroupMember(taskId, groupId, memberId);
    return ApiResponse.ok();
  }

  @PostMapping("/tasks/{taskId}/assessor-groups/{groupId}/dispatch")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.dispatch')")
  @AuditLog(module = "certification", action = "preparation-assessor:dispatch", targetType = "certification_assessor_task")
  public ApiResponse<List<PreparationAssessorTaskVO>> dispatchGroup(
      @PathVariable Long taskId,
      @PathVariable Long groupId,
      @Valid @RequestBody PreparationAssessorDispatchReq req
  ) {
    return ApiResponse.ok(service.dispatchGroup(taskId, groupId, req));
  }
}
