package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserAbilityApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserAbilityVO;
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

/**
 * 个人技能矩阵管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserAbilityController {

  private final TrainUserAbilityApplicationService trainUserAbilityApplicationService;

  public TrainUserAbilityController(TrainUserAbilityApplicationService trainUserAbilityApplicationService) {
    this.trainUserAbilityApplicationService = trainUserAbilityApplicationService;
  }

  /** 分页查询个人技能矩阵列表 */
  @GetMapping("/user-abilities")
  @PreAuthorize("hasAuthority('train:user-ability:view')")
  public ApiResponse<PageResponse<TrainUserAbilityVO>> page(@Valid @ModelAttribute TrainUserAbilityQueryReq req) {
    return ApiResponse.ok(trainUserAbilityApplicationService.page(req));
  }

  /** 根据ID查询个人技能矩阵详情 */
  @GetMapping("/user-abilities/{id}")
  @PreAuthorize("hasAuthority('train:user-ability:view')")
  public ApiResponse<TrainUserAbilityVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainUserAbilityApplicationService.getById(id));
  }

  /** 新增个人技能矩阵 */
  @PostMapping("/user-abilities")
  @PreAuthorize("hasAuthority('train:user-ability:create')")
  @AuditLog(module = "training", action = "user-ability:create", targetType = "train_user_ability")
  public ApiResponse<TrainUserAbilityVO> create(@Valid @RequestBody TrainUserAbilityCreateReq req) {
    return ApiResponse.ok(trainUserAbilityApplicationService.create(req));
  }

  /** 修改个人技能矩阵 */
  @PutMapping("/user-abilities/{id}")
  @PreAuthorize("hasAuthority('train:user-ability:update')")
  @AuditLog(module = "training", action = "user-ability:update", targetType = "train_user_ability")
  public ApiResponse<TrainUserAbilityVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserAbilityUpdateReq req) {
    return ApiResponse.ok(trainUserAbilityApplicationService.update(id, req));
  }

  /** 删除个人技能矩阵 */
  @DeleteMapping("/user-abilities/{id}")
  @PreAuthorize("hasAuthority('train:user-ability:delete')")
  @AuditLog(module = "training", action = "user-ability:delete", targetType = "train_user_ability")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainUserAbilityApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
