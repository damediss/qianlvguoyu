package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionRefereeAssignmentReplaceReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeBatchAssignmentReplaceReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeGroupBindingReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeGroupUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeUpdateReq;
import com.guoyu.platform.competition.service.CompetitionRefereeManagementService;
import com.guoyu.platform.competition.vo.CompetitionRefereeAssignmentWorkspaceVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeBatchAssignmentResultVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeCandidateVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeGroupVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeVO;
import jakarta.validation.Valid;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionRefereeManagementController {

  private final CompetitionRefereeManagementService service;

  public CompetitionRefereeManagementController(CompetitionRefereeManagementService service) {
    this.service = service;
  }

  @GetMapping("/referees")
  @PreAuthorize("hasAuthority('competition:referee:view')")
  public ApiResponse<PageResponse<CompetitionRefereeVO>> page(
      @Valid @ModelAttribute CompetitionRefereeQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/referees/{profileId}")
  @PreAuthorize("hasAuthority('competition:referee:update')")
  public ApiResponse<CompetitionRefereeVO> getForEdit(@PathVariable Long profileId) {
    return ApiResponse.ok(service.getForEdit(profileId));
  }

  @PostMapping("/referees")
  @PreAuthorize("hasAuthority('competition:referee:create')")
  @AuditLog(module = "competition", action = "referee:create", targetType = "competition_referee_profile")
  public ApiResponse<CompetitionRefereeVO> create(
      @Valid @RequestBody CompetitionRefereeCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/referees/{profileId}")
  @PreAuthorize("hasAuthority('competition:referee:update')")
  @AuditLog(module = "competition", action = "referee:update", targetType = "competition_referee_profile")
  public ApiResponse<CompetitionRefereeVO> update(
      @PathVariable Long profileId,
      @Valid @RequestBody CompetitionRefereeUpdateReq req) {
    return ApiResponse.ok(service.update(profileId, req));
  }

  @DeleteMapping("/referees/{profileId}")
  @PreAuthorize("hasAuthority('competition:referee:delete')")
  @AuditLog(module = "competition", action = "referee:delete", targetType = "competition_referee_profile")
  public ApiResponse<Void> delete(@PathVariable Long profileId) {
    service.delete(profileId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/stages/{stageId}/referee-assignments")
  @PreAuthorize("hasAuthority('competition:referee-assignment:view')")
  public ApiResponse<CompetitionRefereeAssignmentWorkspaceVO> assignmentWorkspace(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.assignmentWorkspace(stageId));
  }

  @GetMapping("/referee-groups")
  @PreAuthorize("hasAnyAuthority('competition:referee-group:view', 'competition:referee:view')")
  public ApiResponse<List<CompetitionRefereeGroupVO>> listGroups() {
    return ApiResponse.ok(service.listGroups());
  }

  @GetMapping("/stages/{stageId}/referee-groups")
  @PreAuthorize("hasAuthority('competition:referee-assignment:view')")
  public ApiResponse<List<CompetitionRefereeGroupVO>> groups(@PathVariable Long stageId) {
    return ApiResponse.ok(service.groups(stageId));
  }

  @PostMapping("/referee-groups")
  @PreAuthorize("hasAnyAuthority('competition:referee-group:manage', 'competition:referee:create', 'competition:referee:update')")
  @AuditLog(module = "competition", action = "referee:group-create",
      targetType = "competition_referee_group")
  public ApiResponse<CompetitionRefereeGroupVO> createGroup(
      @Valid @RequestBody CompetitionRefereeGroupUpsertReq req) {
    return ApiResponse.ok(service.createGroup(req));
  }

  @PutMapping("/referee-groups/{groupId}")
  @PreAuthorize("hasAnyAuthority('competition:referee-group:manage', 'competition:referee:create', 'competition:referee:update')")
  @AuditLog(module = "competition", action = "referee:group-update",
      targetType = "competition_referee_group")
  public ApiResponse<CompetitionRefereeGroupVO> updateGroup(
      @PathVariable Long groupId,
      @Valid @RequestBody CompetitionRefereeGroupUpsertReq req) {
    return ApiResponse.ok(service.updateGroup(groupId, req));
  }

  @PostMapping("/referee-groups/{groupId}/confirm")
  @PreAuthorize("hasAnyAuthority('competition:referee-group:manage', 'competition:referee:create', 'competition:referee:update')")
  @AuditLog(module = "competition", action = "referee:group-confirm",
      targetType = "competition_referee_group")
  public ApiResponse<CompetitionRefereeGroupVO> confirmGroup(@PathVariable Long groupId) {
    return ApiResponse.ok(service.confirmGroup(groupId));
  }

  @DeleteMapping("/referee-groups/{groupId}")
  @PreAuthorize("hasAnyAuthority('competition:referee-group:manage', 'competition:referee:delete')")
  @AuditLog(module = "competition", action = "referee:group-delete",
      targetType = "competition_referee_group")
  public ApiResponse<Void> deleteGroup(@PathVariable Long groupId) {
    service.deleteGroup(groupId);
    return ApiResponse.ok(null);
  }

  @PutMapping("/stages/{stageId}/referee-group-bindings/{contestantMemberId}")
  @PreAuthorize("hasAuthority('competition:referee-assignment:manage')")
  @AuditLog(module = "competition", action = "referee:group-binding",
      targetType = "competition_referee_assignment")
  public ApiResponse<CompetitionRefereeCandidateVO> bindGroup(
      @PathVariable Long stageId,
      @PathVariable Long contestantMemberId,
      @RequestBody(required = false) CompetitionRefereeGroupBindingReq req) {
    return ApiResponse.ok(service.bindGroup(stageId, contestantMemberId, req));
  }

  @PutMapping("/stages/{stageId}/referee-assignments/{contestantMemberId}")
  @PreAuthorize("hasAuthority('competition:referee-assignment:manage')")
  @AuditLog(module = "competition", action = "referee:assignment-replace",
      targetType = "competition_referee_assignment")
  public ApiResponse<CompetitionRefereeCandidateVO> replaceAssignments(
      @PathVariable Long stageId,
      @PathVariable Long contestantMemberId,
      @Valid @RequestBody CompetitionRefereeAssignmentReplaceReq req) {
    return ApiResponse.ok(service.replaceAssignments(stageId, contestantMemberId, req));
  }

  @PutMapping("/stages/{stageId}/referee-assignments/batch")
  @PreAuthorize("hasAuthority('competition:referee-assignment:manage')")
  @AuditLog(module = "competition", action = "referee:assignment-batch-replace",
      targetType = "competition_referee_assignment")
  public ApiResponse<CompetitionRefereeBatchAssignmentResultVO> replaceAssignmentsBatch(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionRefereeBatchAssignmentReplaceReq req) {
    return ApiResponse.ok(service.replaceAssignmentsBatch(stageId, req));
  }
}
