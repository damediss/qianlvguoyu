package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.ResetPasswordReq;
import com.guoyu.platform.system.vo.UserVO;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseAccountCreateReq;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseAccountUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseFeatureConfigSaveReq;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyUpdateReq;
import com.guoyu.platform.trainingdepartment.service.EnterpriseFeatureConfigService;
import com.guoyu.platform.trainingdepartment.service.TrainEnterpriseAccountApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSystemCompanyApplicationService;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseAccountRoleVO;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseFeatureConfigVO;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseFeatureOptionsVO;
import com.guoyu.platform.trainingdepartment.vo.SystemCompanyVO;
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
 * 培训管理 · 支持保障 · 企业管理（系统企业 company）及开设账号。
 */
@Validated
@RestController
@RequestMapping("/training/system-companies")
public class TrainSystemCompanyController {

  private final TrainSystemCompanyApplicationService service;
  private final TrainEnterpriseAccountApplicationService accountService;
  private final EnterpriseFeatureConfigService featureConfigService;

  public TrainSystemCompanyController(
      TrainSystemCompanyApplicationService service,
      TrainEnterpriseAccountApplicationService accountService,
      EnterpriseFeatureConfigService featureConfigService) {
    this.service = service;
    this.accountService = accountService;
    this.featureConfigService = featureConfigService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('train:company:view')")
  public ApiResponse<List<SystemCompanyVO>> list(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.list(keyword));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('train:company:create')")
  @AuditLog(module = "training", action = "company:create", targetType = "sys_org")
  public ApiResponse<SystemCompanyVO> create(@Valid @RequestBody SystemCompanyCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('train:company:update')")
  @AuditLog(module = "training", action = "company:update", targetType = "sys_org")
  public ApiResponse<SystemCompanyVO> update(
      @PathVariable Long id, @Valid @RequestBody SystemCompanyUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('train:company:delete')")
  @AuditLog(module = "training", action = "company:delete", targetType = "sys_org")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/feature-options")
  @PreAuthorize("hasAnyAuthority('train:company:feature', 'train:company:account', 'train:company:create', 'train:company:update')")
  public ApiResponse<EnterpriseFeatureOptionsVO> featureOptions() {
    return ApiResponse.ok(featureConfigService.options());
  }

  @GetMapping("/{id}/feature-config")
  @PreAuthorize("hasAnyAuthority('train:company:feature', 'train:company:account', 'train:company:create', 'train:company:update', 'train:company:view')")
  public ApiResponse<EnterpriseFeatureConfigVO> getFeatureConfig(@PathVariable Long id) {
    return ApiResponse.ok(featureConfigService.get(id));
  }

  @PutMapping("/{id}/feature-config")
  @PreAuthorize("hasAnyAuthority('train:company:feature', 'train:company:account', 'train:company:create', 'train:company:update')")
  @AuditLog(module = "training", action = "company:feature-config", targetType = "sys_org")
  public ApiResponse<EnterpriseFeatureConfigVO> saveFeatureConfig(
      @PathVariable Long id, @Valid @RequestBody EnterpriseFeatureConfigSaveReq req) {
    return ApiResponse.ok(featureConfigService.save(id, req));
  }

  @GetMapping("/{id}/account-roles")
  @PreAuthorize("hasAnyAuthority('train:company:account', 'train:company:create')")
  public ApiResponse<List<EnterpriseAccountRoleVO>> accountRoles(@PathVariable Long id) {
    return ApiResponse.ok(accountService.listAssignableRoles(id));
  }

  @GetMapping("/{id}/accounts")
  @PreAuthorize("hasAnyAuthority('train:company:view', 'train:company:account', 'train:company:create')")
  public ApiResponse<PageResponse<UserVO>> listAccounts(
      @PathVariable Long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(accountService.pageAccounts(id, page, pageSize, keyword));
  }

  @PostMapping("/{id}/accounts")
  @PreAuthorize("hasAnyAuthority('train:company:account', 'train:company:create')")
  @AuditLog(module = "training", action = "company:account:create", targetType = "sys_user")
  public ApiResponse<UserVO> createAccount(
      @PathVariable Long id, @Valid @RequestBody EnterpriseAccountCreateReq req) {
    return ApiResponse.ok(accountService.createAccount(id, req));
  }

  @PutMapping("/{id}/accounts/{userId}")
  @PreAuthorize("hasAnyAuthority('train:company:account', 'train:company:create')")
  @AuditLog(module = "training", action = "company:account:update", targetType = "sys_user")
  public ApiResponse<UserVO> updateAccount(
      @PathVariable Long id,
      @PathVariable Long userId,
      @Valid @RequestBody EnterpriseAccountUpdateReq req) {
    return ApiResponse.ok(accountService.updateAccount(id, userId, req));
  }

  @PostMapping("/{id}/accounts/{userId}/status")
  @PreAuthorize("hasAnyAuthority('train:company:account', 'train:company:create')")
  @AuditLog(module = "training", action = "company:account:status", targetType = "sys_user")
  public ApiResponse<UserVO> setAccountStatus(
      @PathVariable Long id,
      @PathVariable Long userId,
      @RequestParam String status) {
    return ApiResponse.ok(accountService.setAccountStatus(id, userId, status));
  }

  @PostMapping("/{id}/accounts/{userId}/reset-password")
  @PreAuthorize("hasAnyAuthority('train:company:account', 'train:company:create')")
  @AuditLog(module = "training", action = "company:account:reset-password", targetType = "sys_user")
  public ApiResponse<Void> resetAccountPassword(
      @PathVariable Long id,
      @PathVariable Long userId,
      @Valid @RequestBody ResetPasswordReq req) {
    accountService.resetPassword(id, userId, req);
    return ApiResponse.ok();
  }
}
