package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.trainingdepartment.config.OnlyOfficeProperties;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialInstanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 修订方案 §2.3 / P1-12：OnlyOffice 回调面加固。
 * 覆盖：未启用即拒绝、下载地址 host 白名单、密钥缺失 fail-fast。
 */
class TrainSubsidyMaterialOfficeApplicationServiceTest {

  private static final String DOCS_HOST = "https://docs.internal.example.com";

  private static TrainSubsidyMaterialOfficeApplicationService service(
      boolean enabled, String documentServerUrl, String jwtSecret) {
    OnlyOfficeProperties props = new OnlyOfficeProperties(
        enabled, documentServerUrl, "http://api.example.com/api", false, jwtSecret, 3600);
    return new TrainSubsidyMaterialOfficeApplicationService(
        props,
        mock(TrainSubsidyMapper.class),
        mock(TrainSubsidyMaterialInstanceMapper.class),
        mock(TrainSubsidyMaterialVersionMapper.class),
        mock(TrainSubsidySlotFileMapper.class),
        mock(TrainSubsidyEventMapper.class),
        mock(FileApplicationService.class),
        new ObjectMapper());
  }

  @Test
  @DisplayName("★ enabled=false 时回调直接 404，不进解析与下载逻辑")
  void rejectsCallbackWhenDisabled() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(false, DOCS_HOST, "secret");
    BusinessException ex = assertThrows(BusinessException.class, () -> svc.requireEnabled());
    assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
  }

  @Test
  @DisplayName("enabled=true 时放行")
  void allowsWhenEnabled() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "secret");
    assertDoesNotThrow(svc::requireEnabled);
  }

  @Test
  @DisplayName("★ 回调 url 指向已配置 Document Server 之外的 host → 拒绝（SSRF）")
  void rejectsForeignCallbackUrl() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "secret");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> svc.requireAllowedDocumentServerHost("http://169.254.169.254/latest/meta-data/"));
    assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
  }

  @Test
  @DisplayName("回调 url host 与配置一致 → 放行")
  void allowsSameHostCallbackUrl() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "secret");
    assertDoesNotThrow(() ->
        svc.requireAllowedDocumentServerHost(DOCS_HOST + "/cache/files/out.docx"));
  }

  @Test
  @DisplayName("未配置 Document Server 地址 → 一律拒绝，不做无锚点放行")
  void rejectsWhenDocumentServerUrlMissing() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, "", "secret");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> svc.requireAllowedDocumentServerHost("https://anything.example.com/x.docx"));
    assertEquals(ErrorCode.INTERNAL_ERROR, ex.getCode());
  }

  @Test
  @DisplayName("url 缺省或非法 → 拒绝")
  void rejectsBlankOrInvalidUrl() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "secret");
    assertEquals(ErrorCode.BAD_REQUEST,
        assertThrows(BusinessException.class, () -> svc.requireAllowedDocumentServerHost(" "))
            .getCode());
    assertEquals(ErrorCode.BAD_REQUEST,
        assertThrows(BusinessException.class, () -> svc.requireAllowedDocumentServerHost("not a url"))
            .getCode());
  }

  @Test
  @DisplayName("★ jwt-secret 未配置即失败，不再回退硬编码 dev secret")
  void failsFastWhenJwtSecretMissing() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "");
    BusinessException ex = assertThrows(BusinessException.class, svc::resolveJwtSecret);
    assertEquals(ErrorCode.INTERNAL_ERROR, ex.getCode());
  }

  @Test
  @DisplayName("jwt-secret 已配置则返回该值")
  void returnsConfiguredSecret() {
    TrainSubsidyMaterialOfficeApplicationService svc = service(true, DOCS_HOST, "configured-secret");
    assertEquals("configured-secret", svc.resolveJwtSecret());
  }
}
