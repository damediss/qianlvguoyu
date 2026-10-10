package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogDraftFromOpsReq;
import com.guoyu.platform.trainingdepartment.dto.FollowWorkLogUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainFollowLogApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowOpLogVO;
import com.guoyu.platform.trainingdepartment.vo.FollowWorkLogVO;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * 管理端跟班过程动态 + 工作日志。
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainFollowLogAdminController {

  private final TrainFollowLogApplicationService logService;

  public TrainFollowLogAdminController(TrainFollowLogApplicationService logService) {
    this.logService = logService;
  }

  @GetMapping("/info-classes/{classId}/follow/op-logs")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowOpLogVO>> listOpLogs(
      @PathVariable Long classId,
      @RequestParam(required = false) String bizDate,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long actorUserId,
      @RequestParam(required = false) String minSeverity) {
    return ApiResponse.ok(
        logService.listOpLogs(classId, bizDate, from, to, category, actorUserId, minSeverity));
  }

  @GetMapping("/info-classes/{classId}/follow/logs")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowWorkLogVO>> listWorkLogs(
      @PathVariable Long classId,
      @RequestParam(required = false) String bizDate,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return ApiResponse.ok(logService.listWorkLogs(classId, bizDate, from, to));
  }

  @PostMapping("/info-classes/{classId}/follow/logs")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-log:create", targetType = "train_follow_log")
  public ApiResponse<FollowWorkLogVO> create(
      @PathVariable Long classId, @Valid @RequestBody FollowWorkLogCreateReq body) {
    return ApiResponse.ok(logService.createWorkLog(classId, body));
  }

  @PutMapping("/follow-logs/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-log:update", targetType = "train_follow_log")
  public ApiResponse<FollowWorkLogVO> update(
      @PathVariable Long id, @RequestBody FollowWorkLogUpdateReq body) {
    return ApiResponse.ok(logService.updateWorkLog(id, body));
  }

  @DeleteMapping("/follow-logs/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-log:delete", targetType = "train_follow_log")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    logService.deleteWorkLog(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/info-classes/{classId}/follow/logs/draft-from-ops")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-log:draft-from-ops", targetType = "train_follow_log")
  public ApiResponse<FollowWorkLogVO> draftFromOps(
      @PathVariable Long classId, @Valid @RequestBody FollowWorkLogDraftFromOpsReq body) {
    return ApiResponse.ok(logService.draftFromOps(classId, body));
  }
}
