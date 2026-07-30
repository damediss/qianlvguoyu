package com.guoyu.platform.chazheng.controller;

import com.guoyu.platform.chazheng.dto.CertCheckBatchProgress;
import com.guoyu.platform.chazheng.dto.CertCheckBatchReport;
import com.guoyu.platform.chazheng.dto.CertCheckBatchSubmitResp;
import com.guoyu.platform.chazheng.dto.ChazhengUserQueryReq;
import com.guoyu.platform.chazheng.entity.ChazhengUserEntity;
import com.guoyu.platform.chazheng.mapper.ChazhengUserMapper;
import com.guoyu.platform.chazheng.service.ChazhengApplicationService;
import com.guoyu.platform.chazheng.vo.ChazhengUserVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/chazheng")
@Slf4j
public class ChazhengController {

  private final ChazhengApplicationService service;
  private final ChazhengUserMapper userMapper;

  /** 线程池 */
  private ThreadPoolTaskExecutor chazhengCertCheckExecutor;

  public ChazhengController(ChazhengApplicationService service,
      ChazhengUserMapper userMapper) {
    this.service = service;
    this.userMapper = userMapper;
  }

  @PostConstruct
  public void init() {
    // 创建线程池并注入到 service
    chazhengCertCheckExecutor = new ThreadPoolTaskExecutor();
    chazhengCertCheckExecutor.setCorePoolSize(3);
    chazhengCertCheckExecutor.setMaxPoolSize(5);
    chazhengCertCheckExecutor.setQueueCapacity(1000);
    chazhengCertCheckExecutor.setThreadNamePrefix("cert-check-");
    chazhengCertCheckExecutor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    chazhengCertCheckExecutor.setWaitForTasksToCompleteOnShutdown(true);
    chazhengCertCheckExecutor.setAwaitTerminationSeconds(60);
    chazhengCertCheckExecutor.initialize();
    service.setExecutor(chazhengCertCheckExecutor);
  }

  // ── 用户列表 / 导入 / 导出 ──

  @GetMapping("/users")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<PageResponse<ChazhengUserVO>> page(@Valid @ModelAttribute ChazhengUserQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/users/ymd")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<List<String>> getYmdList() {
    return ApiResponse.ok(service.getYmdList());
  }

  @GetMapping("/enterprises")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<List<Integer>> getEnterprises() {
    return ApiResponse.ok(service.getEnterprises());
  }

  @GetMapping("/template")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public org.springframework.http.ResponseEntity<byte[]> downloadTemplate() {
    byte[] data = service.generateTemplate();
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "import_user_example.xlsx");
    return org.springframework.http.ResponseEntity.ok().headers(headers).body(data);
  }

  /**
   * 第一步：导入用户 Excel → 只入库，返回 ymd（秒回）。
   * 前端/Python 拿到 ymd 后，再调 {@link #triggerCertByYmd(String, String)} 触发查证。
   */
  @PostMapping("/users/import")
  @PreAuthorize("hasAuthority('chazheng:user:import')")
  @AuditLog(module = "chazheng", action = "user:import", targetType = "chazheng_user")
  public ApiResponse<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
    int count = service.importExcel(file);
    if (count == 0) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.BAD_REQUEST,
          "导入无可处理的数据，请检查 Excel 内容（姓名+18 位身份证为必填）");
    }

    String latestYmd = service.getLatestYmd();
    Map<String, Object> data = new HashMap<>();
    data.put("imported", count);
    data.put("ymd", latestYmd);
    data.put("message", "导入成功，请调用 /chazheng/users/import/" + latestYmd + "/cert 触发查证");
    return ApiResponse.ok(data);
  }

  /**
   * 第二步：按 ymd 触发异步查证 → 返回 batchId（秒回）。
   * 前端/Python 拿到 batchId 后，轮询 /chazheng/cert/batch/{batchId}/progress 获取进度。
   */
  @PostMapping("/users/import/{ymd}/cert")
  @PreAuthorize("hasAuthority('chazheng:user:cert')")
  @AuditLog(module = "chazheng", action = "user:import:cert", targetType = "chazheng_cert_batch")
  public ApiResponse<Map<String, Object>> triggerCertByYmd(
      @PathVariable String ymd,
      @RequestParam(required = false) String sourceFilename) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    List<ChazhengUserEntity> importedUsers = userMapper.selectList(
        new LambdaQueryWrapper<ChazhengUserEntity>()
            .eq(ChazhengUserEntity::getTenantId, tenantId)
            .eq(ChazhengUserEntity::getYmd, ymd));

    List<Long> userIds = importedUsers.stream()
        .map(ChazhengUserEntity::getId)
        .map(Integer::longValue)
        .toList();

    if (userIds.isEmpty()) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.BAD_REQUEST,
          "该 ymd 下无用户可查证");
    }

    String filename = (sourceFilename != null && !sourceFilename.isBlank())
        ? sourceFilename : ("import_" + ymd + ".xlsx");
    CertCheckBatchSubmitResp batchResp = service.submitCertCheckBatch(userIds, filename);

    Map<String, Object> data = new HashMap<>();
    data.put("batchId", batchResp.getBatchId());
    data.put("totalCount", batchResp.getTotalCount());
    data.put("status", "processing");
    data.put("message", batchResp.getMessage());
    return ApiResponse.ok(data);
  }

  /**
   * 单个用户查证(原有接口,保留)。
   */
  @PostMapping("/users/{id}/cert")
  @PreAuthorize("hasAuthority('chazheng:user:cert')")
  @AuditLog(module = "chazheng", action = "user:cert", targetType = "chazheng_user")
  public ApiResponse<ChazhengUserVO> certCheck(@PathVariable Integer id) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(service.certCheck(id, tenantId));
  }

  @GetMapping("/users/cert/progress")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<Map<String, Long>> getCertProgress() {
    return ApiResponse.ok(service.getCertProgress());
  }

  @PostMapping("/users/export")
  @PreAuthorize("hasAuthority('chazheng:user:export')")
  @AuditLog(module = "chazheng", action = "user:export", targetType = "chazheng_user")
  public org.springframework.http.ResponseEntity<byte[]> exportUsers(
      @RequestParam(required = false) String ymd,
      @RequestParam(required = false) String ids) {
    List<Integer> idList = null;
    if (ids != null && !ids.isBlank()) {
      idList = Arrays.stream(ids.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Integer::valueOf)
          .toList();
    }
    byte[] data = service.exportExcel(ymd, idList);
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "chazheng_users.xlsx");
    return org.springframework.http.ResponseEntity.ok().headers(headers).body(data);
  }

  // ── 批量查证新接口 ──

  /**
   * 查询批次查证进度(前端轮询)。
   */
  @GetMapping("/cert/batch/{batchId}/progress")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<CertCheckBatchProgress> getBatchProgress(@PathVariable Long batchId) {
    return ApiResponse.ok(service.getBatchProgress(batchId));
  }

  /**
   * 查询批次查证报告。
   */
  @GetMapping("/cert/batch/{batchId}/report")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<CertCheckBatchReport> getBatchReport(@PathVariable Long batchId) {
    return ApiResponse.ok(service.getBatchReport(batchId));
  }

  /**
   * 重试批次中的查证失败用户。
   */
  @PostMapping("/cert/batch/{batchId}/retry")
  @PreAuthorize("hasAuthority('chazheng:user:cert')")
  @AuditLog(module = "chazheng", action = "batch:retry", targetType = "chazheng_cert_batch")
  public ApiResponse<CertCheckBatchSubmitResp> retryFailed(@PathVariable Long batchId) {
    return ApiResponse.ok(service.retryFailedBatch(batchId));
  }
}
