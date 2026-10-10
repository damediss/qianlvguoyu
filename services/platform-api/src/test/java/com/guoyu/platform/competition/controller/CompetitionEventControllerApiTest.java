package com.guoyu.platform.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.competition.dto.CompetitionEventCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.service.CompetitionEventApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionEventControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionEventApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionEventApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionEventController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void pageReturnsUnifiedPageResponseAndPassesFilters() throws Exception {
    CompetitionEventVO event = event(1001L);
    when(service.page(any(CompetitionEventQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(event), 1, 10, 1));

    mockMvc.perform(get("/competition/events")
            .param("keyword", "spring")
            .param("scope", "platform")
            .param("status", "draft")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value("1001"))
        .andExpect(jsonPath("$.data.items[0].status").value("draft"));

    ArgumentCaptor<CompetitionEventQueryReq> captor = ArgumentCaptor.forClass(CompetitionEventQueryReq.class);
    verify(service).page(captor.capture());
    Assertions.assertThat(captor.getValue().getKeyword()).isEqualTo("spring");
    Assertions.assertThat(captor.getValue().getScope()).isEqualTo("platform");
    Assertions.assertThat(captor.getValue().getStatus()).isEqualTo("draft");
  }

  @Test
  void createReturnsCreatedDraftEvent() throws Exception {
    CompetitionEventVO event = event(1002L);
    when(service.create(any(CompetitionEventCreateReq.class))).thenReturn(event);

    mockMvc.perform(post("/competition/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Spring Competition",
                  "code": "SPRING-2026",
                  "scope": "platform",
                  "registrationAuditRequired": 0,
                  "startAt": "2026-08-01T09:00:00",
                  "endAt": "2026-08-02T18:00:00",
                  "description": "draft event"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.id").value("1002"))
        .andExpect(jsonPath("$.data.status").value("draft"));

    verify(service).create(any(CompetitionEventCreateReq.class));
  }

  @Test
  void updateDraftReturnsUpdatedEvent() throws Exception {
    CompetitionEventVO event = event(1003L);
    event.setName("Updated Competition");
    when(service.update(Mockito.eq(1003L), any(CompetitionEventUpdateReq.class))).thenReturn(event);

    mockMvc.perform(put("/competition/events/1003")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Updated Competition",
                  "registrationAuditRequired": 1,
                  "startAt": "2026-08-01T09:00:00",
                  "endAt": "2026-08-03T18:00:00",
                  "description": "updated draft"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.name").value("Updated Competition"));

    verify(service).update(Mockito.eq(1003L), any(CompetitionEventUpdateReq.class));
  }

  @Test
  void createRejectsMissingRequiredFieldsBeforeCallingService() throws Exception {
    mockMvc.perform(post("/competition/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "scope": "platform"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.message").exists());

    verifyNoInteractions(service);
  }

  @Test
  void publishEndpointReturnsCommandResultWithoutRequestBody() throws Exception {
    CompetitionEventPublishResultVO result = new CompetitionEventPublishResultVO();
    result.setEventId(1004L);
    result.setStatus("published");
    when(service.publish(1004L)).thenReturn(result);

    mockMvc.perform(post("/competition/events/1004/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.eventId").value("1004"))
        .andExpect(jsonPath("$.data.status").value("published"));

    verify(service).publish(1004L);
  }

  @Test
  void publishEndpointDoesNotAcceptClientManagedFields() throws Exception {
    mockMvc.perform(post("/competition/events/1004/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"published\",\"tenantId\":1}"))
        .andExpect(status().isOk());

    verify(service).publish(1004L);
  }

  @Test
  void publishEndpointIsExposedWithOnlyThePublishCommand() {
    Assertions.assertThat(Arrays.stream(CompetitionEventController.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName))
        .contains("publish");
  }

  @Test
  void publishValidationFailureReturns422() throws Exception {
    when(service.publish(1004L)).thenThrow(
        new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "缺少有效赛段"));

    mockMvc.perform(post("/competition/events/1004/publish"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("422"));
  }

  @Test
  void publishReadinessEndpointReturnsStructuredScopeMatrix() throws Exception {
    when(service.publishReadiness(1004L)).thenReturn(new CompetitionEventPublishReadinessVO(
        1004L, "EVENT-1004", "赛事", "draft", "platform",
        LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 2, 18, 0),
        false, 1, 1, 0, 0, List.of(), List.of()));

    mockMvc.perform(get("/competition/events/1004/publish-readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.eventId").value("1004"))
        .andExpect(jsonPath("$.data.ready").value(false))
        .andExpect(jsonPath("$.data.selectedLevelCount").value(1));

    verify(service).publishReadiness(1004L);
  }

  private CompetitionEventVO event(Long id) {
    CompetitionEventVO event = new CompetitionEventVO();
    event.setId(id);
    event.setName("Competition " + id);
    event.setCode("EVENT-" + id);
    event.setScope("platform");
    event.setStatus("draft");
    event.setRegistrationAuditRequired(0);
    event.setStartAt(LocalDateTime.of(2026, 8, 1, 9, 0));
    event.setEndAt(LocalDateTime.of(2026, 8, 2, 18, 0));
    return event;
  }
}
