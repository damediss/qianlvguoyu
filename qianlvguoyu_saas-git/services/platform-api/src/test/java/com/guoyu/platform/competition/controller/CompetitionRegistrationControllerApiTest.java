package com.guoyu.platform.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.dto.CompetitionRegistrationAuditRequest;
import com.guoyu.platform.competition.dto.CompetitionRegistrationQueryReq;
import com.guoyu.platform.competition.service.CompetitionRegistrationApplicationService;
import com.guoyu.platform.competition.vo.CompetitionRegistrationAuditResultVO;
import com.guoyu.platform.competition.vo.CompetitionRegistrationVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionRegistrationControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionRegistrationApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionRegistrationApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionRegistrationController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void pageReturnsRegistrations() throws Exception {
    CompetitionRegistrationVO registration = new CompetitionRegistrationVO();
    registration.setRegistrationId(9101L);
    registration.setEventId(1001L);
    registration.setOrgId(3001L);
    registration.setName("Contestant");
    registration.setStatus("pending_audit");
    when(service.page(any(CompetitionRegistrationQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(registration), 1, 20, 1));

    mockMvc.perform(get("/competition/registrations")
            .param("eventId", "1001")
            .param("orgId", "3001")
            .param("status", "pending_audit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].registrationId").value(9101))
        .andExpect(jsonPath("$.data.items[0].status").value("pending_audit"));

    ArgumentCaptor<CompetitionRegistrationQueryReq> captor =
        ArgumentCaptor.forClass(CompetitionRegistrationQueryReq.class);
    verify(service).page(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getEventId()).isEqualTo(1001L);
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrgId()).isEqualTo(3001L);
  }

  @Test
  void auditReturnsStateTransition() throws Exception {
    CompetitionRegistrationAuditResultVO result = new CompetitionRegistrationAuditResultVO();
    result.setRegistrationId(9101L);
    result.setEventId(1001L);
    result.setOrgId(3001L);
    result.setAction("approve");
    result.setBeforeStatus("pending_audit");
    result.setStatus("eligible_unactivated");
    when(service.audit(Mockito.eq(9101L), any(CompetitionRegistrationAuditRequest.class)))
        .thenReturn(result);

    mockMvc.perform(post("/competition/registrations/9101/audit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "action": "approve",
                  "reason": "符合要求"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.registrationId").value(9101))
        .andExpect(jsonPath("$.data.beforeStatus").value("pending_audit"))
        .andExpect(jsonPath("$.data.status").value("eligible_unactivated"));

    verify(service).audit(Mockito.eq(9101L), any(CompetitionRegistrationAuditRequest.class));
  }

  @Test
  void auditReturnsConflictForIllegalBusinessState() throws Exception {
    when(service.audit(Mockito.eq(9101L), any(CompetitionRegistrationAuditRequest.class)))
        .thenThrow(new BusinessException(ErrorCode.CONFLICT, "当前参赛资格状态不允许审核"));

    mockMvc.perform(post("/competition/registrations/9101/audit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\":\"approve\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.message").value("当前参赛资格状态不允许审核"));
  }

  @Test
  void pageRejectsInvalidPageBeforeCallingService() throws Exception {
    mockMvc.perform(get("/competition/registrations")
            .param("page", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }

  @Test
  void auditRejectsMalformedJsonBeforeCallingService() throws Exception {
    mockMvc.perform(post("/competition/registrations/9101/audit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }
}
