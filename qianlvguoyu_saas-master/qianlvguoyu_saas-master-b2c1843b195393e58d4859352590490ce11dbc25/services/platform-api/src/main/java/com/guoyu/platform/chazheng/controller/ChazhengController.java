package com.guoyu.platform.chazheng.controller;

import com.guoyu.platform.chazheng.dto.ChazhengUserQueryReq;
import com.guoyu.platform.chazheng.service.ChazhengApplicationService;
import com.guoyu.platform.chazheng.vo.ChazhengUserVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@Validated
@RestController
@RequestMapping("/chazheng")
@Slf4j
public class ChazhengController {

  private final ChazhengApplicationService service;

  public ChazhengController(ChazhengApplicationService service) {
    this.service = service;
  }

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
  public ResponseEntity<byte[]> downloadTemplate() {
    byte[] data = service.generateTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "import_user_example.xlsx");
    return ResponseEntity.ok().headers(headers).body(data);
  }

  @PostMapping("/users/import")
  @PreAuthorize("hasAuthority('chazheng:user:import')")
  @AuditLog(module = "chazheng", action = "user:import", targetType = "chazheng_user")
  public ApiResponse<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
    int count = service.importExcel(file);
    return ApiResponse.ok(Map.of("imported", count));
  }

  @PostMapping("/users/{id}/cert")
  @PreAuthorize("hasAuthority('chazheng:user:cert')")
  @AuditLog(module = "chazheng", action = "user:cert", targetType = "chazheng_user")
  public ApiResponse<ChazhengUserVO> certCheck(@PathVariable Integer id) {
    return ApiResponse.ok(service.certCheck(id));
  }

  @GetMapping("/users/cert/progress")
  @PreAuthorize("hasAuthority('chazheng:user:view')")
  public ApiResponse<Map<String, Long>> getCertProgress() {
    return ApiResponse.ok(service.getCertProgress());
  }

  @PostMapping("/users/export")
  @PreAuthorize("hasAuthority('chazheng:user:export')")
  @AuditLog(module = "chazheng", action = "user:export", targetType = "chazheng_user")
  public ResponseEntity<byte[]> exportUsers(
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
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "chazheng_users.xlsx");
    return ResponseEntity.ok().headers(headers).body(data);
  }
}