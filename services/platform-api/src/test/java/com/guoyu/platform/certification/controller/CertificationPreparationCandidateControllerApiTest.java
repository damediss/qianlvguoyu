package com.guoyu.platform.certification.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.certification.service.CertificationCandidateCsvImportService;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CertificationPreparationCandidateControllerApiTest {

  private MockMvc mockMvc;
  private CertificationCandidateCsvImportService excelImportService;

  @BeforeEach
  void setUp() {
    excelImportService = Mockito.mock(CertificationCandidateCsvImportService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CertificationPreparationCandidateController(
            Mockito.mock(CertificationPreparationApplicationService.class), excelImportService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void downloadExcelTemplateReturnsUtf8EncodedAttachment() throws Exception {
    byte[] template = "xlsx-content".getBytes(StandardCharsets.UTF_8);
    when(excelImportService.templateBytes()).thenReturn(template);

    mockMvc.perform(get("/certification/preparation/candidate-import-template"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            containsString("filename*=UTF-8''%E8%80%83%E7%94%9F%E4%BF%A1%E6%81%AF%E6%89%B9%E9%87%8F%E5%AF%BC%E5%85%A5%E6%A8%A1%E6%9D%BF.xlsx")))
        .andExpect(content().bytes(template));

    verify(excelImportService).templateBytes();
  }
}
