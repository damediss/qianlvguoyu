package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.PersonnelCertLookupReq;
import com.guoyu.platform.trainingdepartment.service.PersonnelCertLookupApplicationService;
import com.guoyu.platform.trainingdepartment.vo.PersonnelCertLookupResultVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training/personnel-cert")
public class PersonnelCertLookupController {

  private final PersonnelCertLookupApplicationService lookupService;

  public PersonnelCertLookupController(PersonnelCertLookupApplicationService lookupService) {
    this.lookupService = lookupService;
  }

  /**
   * 方案规划「人员认定」：按姓名+身份证同步查证，不落库。
   */
  @PostMapping("/verify-lookup")
  @PreAuthorize(
      "hasAnyAuthority('pxanfangguihua:view', 'pxanfangguihua:manage', "
          + "'pxqiyeshenhe:view', 'pxqiyeshenhe:confirm', 'pxanfangguihua:enterprise_confirm')")
  @AuditLog(module = "training", action = "personnel-cert:verify-lookup", targetType = "personnel_cert_lookup")
  public ApiResponse<PersonnelCertLookupResultVO> verifyLookup(
      @Valid @RequestBody PersonnelCertLookupReq req) {
    return ApiResponse.ok(lookupService.lookup(req));
  }
}
