package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.dto.SignUpRegisterReq;
import com.guoyu.platform.trainingdepartment.dto.SignUpMineVO;
import com.guoyu.platform.trainingdepartment.service.SignUpRegisterTestService;
import com.guoyu.platform.trainingdepartment.vo.SignUpDetailVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpRegisterResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 扫码信息收集 - 测试用 controller（完整字段版）。
 *
 * <p>无登录态、跨租户允许访问，仅用于本地联调与功能验证。
 * 生产环境务必配合 TenantContextFilter + 权限校验使用。
 */
@RestController
@RequestMapping("/training/sign-ups")
@RequiredArgsConstructor
@Validated
public class SignUpRegisterTestController {

  private final SignUpRegisterTestService service;

  @GetMapping("/{id}/detail")
  public ApiResponse<SignUpDetailVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(service.getDetail(id));
  }

  /** 通用文件上传 - 接收 multipart/form-data 的 file 字段，返回公网 URL */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadResultVO> upload(@RequestPart("file") MultipartFile file) {
    String url = service.uploadFile(file);
    return ApiResponse.ok(new UploadResultVO(url));
  }

  /** 提交报名（完整字段） */
  @PostMapping(value = "/{id}/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<SignUpRegisterResultVO> register(
      @PathVariable Long id,
      @Valid @RequestBody SignUpRegisterReq req) {
    return ApiResponse.ok(service.register(id, req));
  }

  // ==================== 扫码端·"我的报名" ====================

  /**
   * 根据手机号 + 身份证号查"我"在该计划下的报名记录。
   * 用 query 参数（不用 body）方便前端 GET 请求。
   */
  @GetMapping("/{id}/mine")
  public ApiResponse<SignUpMineVO> findMine(
      @PathVariable Long id,
      @RequestParam @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
      @RequestParam @NotBlank @Pattern(regexp = "^\\d{17}[\\dXx]$") String idCardNumber) {
    return ApiResponse.ok(service.findMine(id, phone, idCardNumber));
  }

  /**
   * 扫码端"修改我的报名"。身份校验用 query 参数里的 phone+idCardNumber，
   * body 携带要修改的字段（与 register 一致）。
   */
  @PutMapping(value = "/{id}/mine", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<SignUpRegisterResultVO> updateMine(
      @PathVariable Long id,
      @RequestParam @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
      @RequestParam @NotBlank @Pattern(regexp = "^\\d{17}[\\dXx]$") String idCardNumber,
      @Valid @RequestBody SignUpRegisterReq req) {
    return ApiResponse.ok(service.updateMine(id, phone, idCardNumber, req));
  }

  public record UploadResultVO(String url) {}
}
