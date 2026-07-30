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

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionStageControllerApiTest {

  private MockMvc mockMvc;
  private CompetitionStageApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionStageApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CompetitionStageController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void pageReturnsStages() throws Exception {
    CompetitionStageVO stage = stage(5001L, "Theory");
    when(service.page(Mockito.eq(1001L), any(CompetitionStageQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(stage), 1, 20, 1));

    mockMvc.perform(get("/competition/events/1001/stages")
            .param("status", "draft")
            .param("page", "1")
            .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(5001))
        .andExpect(jsonPath("$.data.items[0].name").value("Theory"));

    ArgumentCaptor<CompetitionStageQueryReq> captor =
        ArgumentCaptor.forClass(CompetitionStageQueryReq.class);
    verify(service).page(Mockito.eq(1001L), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus()).isEqualTo("draft");
  }

  @Test
  void createReturnsCreatedStage() throws Exception {
    when(service.create(Mockito.eq(1001L), any(CompetitionStageCreateReq.class)))
        .thenReturn(stage(5002L, "Practice"));

    mockMvc.perform(post("/competition/events/1001/stages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Practice",
                  "stageType": "practice",
                  "sortOrder": 2
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.id").value(5002))
        .andExpect(jsonPath("$.data.name").value("Practice"));

    verify(service).create(Mockito.eq(1001L), any(CompetitionStageCreateReq.class));
  }

  @Test
  void updateReturnsUpdatedStage() throws Exception {
    CompetitionStageVO updated = stage(5001L, "Final Practice");
    updated.setStatus("active");
    when(service.update(Mockito.eq(5001L), any(CompetitionStageUpdateReq.class)))
        .thenReturn(updated);

    mockMvc.perform(put("/competition/stages/5001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Final Practice",
                  "stageType": "practice",
                  "sortOrder": 3,
                  "status": "active"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.name").value("Final Practice"))
        .andExpect(jsonPath("$.data.status").value("active"));

    verify(service).update(Mockito.eq(5001L), any(CompetitionStageUpdateReq.class));
  }

  @Test
  void createRejectsBlankNameBeforeCallingService() throws Exception {
    mockMvc.perform(post("/competition/events/1001/stages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": " ",
                  "stageType": "practice",
                  "sortOrder": 1
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }

  @Test
  void pageRejectsOversizedPageBeforeCallingService() throws Exception {
    mockMvc.perform(get("/competition/events/1001/stages")
            .param("pageSize", "501"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));

    verifyNoInteractions(service);
  }

  private CompetitionStageVO stage(Long id, String name) {
    CompetitionStageVO stage = new CompetitionStageVO();
    stage.setId(id);
    stage.setEventId(1001L);
    stage.setName(name);
    stage.setStageType("practice");
    stage.setSortOrder(1);
    stage.setStatus("draft");
    return stage;
  }
}
