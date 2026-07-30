package com.guoyu.platform.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.dto.CompetitionRosterImportQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterImportListQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterPrecheckReq;
import com.guoyu.platform.competition.service.CompetitionRosterImportApplicationService;
import com.guoyu.platform.competition.vo.CompetitionRosterImportDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRosterImportVO;
import com.guoyu.platform.competition.vo.CompetitionRosterItemVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionRosterImportControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionRosterImportApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionRosterImportApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionRosterImportController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void precheckAndConfirmReturnUnifiedResponse() throws Exception {
    CompetitionRosterImportDetailVO detail = detail();
    when(service.precheck(Mockito.eq(1001L), any(CompetitionRosterPrecheckReq.class))).thenReturn(detail);
    when(service.confirm(Mockito.eq(7001L))).thenReturn(detail);

    mockMvc.perform(post("/competition/events/1001/roster-imports/precheck")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fileId\":8001,\"orgId\":3001}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.status").value("pending_confirm"))
        .andExpect(jsonPath("$.data.items.total").value(1));
    mockMvc.perform(post("/competition/roster-imports/7001/confirm"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"));

    verify(service).precheck(Mockito.eq(1001L), any(CompetitionRosterPrecheckReq.class));
    verify(service).confirm(7001L);
  }

  @Test
  void batchQueryReturnsPageResponseForItems() throws Exception {
    when(service.getBatch(Mockito.eq(7001L), any(CompetitionRosterImportQueryReq.class)))
        .thenReturn(detail());

    mockMvc.perform(get("/competition/roster-imports/7001")
            .param("page", "1")
            .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.items.items[0].employeeNo").value("E001"));
  }

  @Test
  void batchListReturnsPageResponseWithoutManualBatchId() throws Exception {
    CompetitionRosterImportVO batch = new CompetitionRosterImportVO();
    batch.setId(7001L);
    batch.setBatchId(7001L);
    batch.setBatchNo("ROSTER-1");
    batch.setEventId(1001L);
    batch.setOrgId(3001L);
    batch.setStatus("pending_confirm");
    when(service.page(Mockito.eq(1001L), any(CompetitionRosterImportListQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(batch), 1, 20, 1));

    mockMvc.perform(get("/competition/events/1001/roster-imports")
            .param("page", "1")
            .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].batchId").value(7001))
        .andExpect(jsonPath("$.data.items[0].batchNo").value("ROSTER-1"));
    verify(service).page(Mockito.eq(1001L), any(CompetitionRosterImportListQueryReq.class));
  }

  private CompetitionRosterImportDetailVO detail() {
    CompetitionRosterItemVO item = new CompetitionRosterItemVO();
    item.setId(9001L);
    item.setBatchId(7001L);
    item.setEmployeeNo("E001");
    item.setName("Alice");
    item.setValidationStatus("valid");
    CompetitionRosterImportDetailVO detail = new CompetitionRosterImportDetailVO();
    detail.setId(7001L);
    detail.setEventId(1001L);
    detail.setOrgId(3001L);
    detail.setStatus("pending_confirm");
    detail.setItems(new PageResponse<>(List.of(item), 1, 20, 1));
    return detail;
  }
}
