package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainCertificateService;
import com.guoyu.platform.trainingdepartment.vo.TrainCertificateVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训证书管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainCertificateController {

  private final TrainCertificateService service;

  public TrainCertificateController(TrainCertificateService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/certificates")
  @PreAuthorize("hasAuthority('train:certificate:view')")
  public ApiResponse<PageResponse<TrainCertificateVO>> page(@Valid @ModelAttribute TrainCertificateQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/certificates/{id}")
  @PreAuthorize("hasAuthority('train:certificate:view')")
  public ApiResponse<TrainCertificateVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/certificates")
  @PreAuthorize("hasAuthority('train:certificate:create')")
  @AuditLog(module = "training", action = "certificate:create", targetType = "train_certificate")
  public ApiResponse<TrainCertificateVO> create(@Valid @RequestBody TrainCertificateCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/certificates/{id}")
  @PreAuthorize("hasAuthority('train:certificate:update')")
  @AuditLog(module = "training", action = "certificate:update", targetType = "train_certificate")
  public ApiResponse<TrainCertificateVO> update(@PathVariable Long id, @Valid @RequestBody TrainCertificateUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/certificates/{id}")
  @PreAuthorize("hasAuthority('train:certificate:delete')")
  @AuditLog(module = "training", action = "certificate:delete", targetType = "train_certificate")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  /** 批量颁发证书 */
  @PostMapping("/certificates/batch-issue")
  @PreAuthorize("hasAuthority('train:certificate:create')")
  @AuditLog(module = "training", action = "certificate:batch-issue", targetType = "train_certificate")
  public ApiResponse<List<TrainCertificateVO>> batchIssue(@RequestBody Map<String, Object> body) {
    List<Long> studentIds = ((List<?>) body.get("studentIds")).stream()
        .map(o -> Long.valueOf(o.toString())).toList();
    Long projectId = Long.valueOf(body.get("projectId").toString());
    return ApiResponse.ok(service.batchIssue(studentIds, projectId));
  }
}
