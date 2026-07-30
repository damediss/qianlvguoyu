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

import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgStatusUpdateReq;
import com.guoyu.platform.competition.service.CompetitionEventOrgApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventOrgVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionEventOrgControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionEventOrgApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionEventOrgApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionEventOrgController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void pageReturnsParticipatingOrganizations() throws Exception {
    CompetitionEventOrgVO relation = relation(2001L, 3001L);
    relation.setStatus("active");
    when(service.page(Mockito.eq(1001L), any(CompetitionEventOrgQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(relation), 1, 20, 1));

    mockMvc.perform(get("/competition/events/1001/organizations")
            .param("status", "active")
            .param("page", "1")
            .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].orgId").value(3001))
        .andExpect(jsonPath("$.data.items[0].status").value("active"));

    ArgumentCaptor<CompetitionEventOrgQueryReq> captor =
        ArgumentCaptor.forClass(CompetitionEventOrgQueryReq.class);
    verify(service).page(Mockito.eq(1001L), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus()).isEqualTo("active");
  }

  @Test
  void createOrganizationReturnsInvitedRelation() throws Exception {
    when(service.create(Mockito.eq(1001L), any(CompetitionEventOrgCreateReq.class)))
        .thenReturn(relation(2002L, 3001L));

    mockMvc.perform(post("/competition/events/1001/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "orgId": 3001,
                  "quota": 20,
                  "importPermission": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.orgId").value(3001))
        .andExpect(jsonPath("$.data.status").value("invited"));

    verify(service).create(Mockito.eq(1001L), any(CompetitionEventOrgCreateReq.class));
  }

  @Test
  void updateOrganizationStatusReturnsUpdatedRelation() throws Exception {
    CompetitionEventOrgVO relation = relation(2003L, 3001L);
    relation.setStatus("active");
    when(service.updateStatus(Mockito.eq(1001L), Mockito.eq(3001L), any(CompetitionEventOrgStatusUpdateReq.class)))
        .thenReturn(relation);

    mockMvc.perform(put("/competition/events/1001/organizations/3001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"active\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.status").value("active"));

    verify(service).updateStatus(Mockito.eq(1001L), Mockito.eq(3001L), any(CompetitionEventOrgStatusUpdateReq.class));
  }

  @Test
  void createRejectsMissingOrganizationBeforeCallingService() throws Exception {
    mockMvc.perform(post("/competition/events/1001/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }

  private CompetitionEventOrgVO relation(Long id, Long orgId) {
    CompetitionEventOrgVO relation = new CompetitionEventOrgVO();
    relation.setId(id);
    relation.setEventId(1001L);
    relation.setOrgId(orgId);
    relation.setOrgName("Company " + orgId);
    relation.setQuota(20);
    relation.setStatus("invited");
    relation.setImportPermission(1);
    return relation;
  }
}
