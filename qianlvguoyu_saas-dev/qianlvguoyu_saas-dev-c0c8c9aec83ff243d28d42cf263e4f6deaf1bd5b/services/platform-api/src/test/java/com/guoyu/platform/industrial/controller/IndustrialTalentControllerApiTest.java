package com.guoyu.platform.industrial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.industrial.dto.IndustrialTalentTrainingQueryReq;
import com.guoyu.platform.industrial.service.IndustrialTalentApplicationService;
import com.guoyu.platform.industrial.vo.IndustrialTalentTrainingVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IndustrialTalentControllerApiTest {

  private MockMvc mockMvc;
  private IndustrialTalentApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(IndustrialTalentApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new IndustrialTalentController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void talentTrainingEndpointReturnsPagedTrainingRows() throws Exception {
    IndustrialTalentTrainingVO item = new IndustrialTalentTrainingVO(
        "800001", "千铝培训", "高级工", "电解槽操作工", "班组长", "大专及以上");
    when(service.pageTraining(any(IndustrialTalentTrainingQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(item), 1, 10, 10));

    mockMvc.perform(get("/industrial/talenttraining")
            .param("keyword", "千铝")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(10))
        .andExpect(jsonPath("$.data.items[0].name").value("千铝培训"))
        .andExpect(jsonPath("$.data.items[0].level").value("高级工"))
        .andExpect(jsonPath("$.data.items[0].work_type").value("电解槽操作工"))
        .andExpect(jsonPath("$.data.items[0].education").value("大专及以上"));

    ArgumentCaptor<IndustrialTalentTrainingQueryReq> captor =
        ArgumentCaptor.forClass(IndustrialTalentTrainingQueryReq.class);
    verify(service).pageTraining(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getKeyword()).isEqualTo("千铝");
  }

  @Test
  void talentServiceEndpointReturnsPagedServiceRows() throws Exception {
    when(service.pageService(any()))
        .thenReturn(new PageResponse<>(
            List.of(new com.guoyu.platform.industrial.vo.IndustrialTalentServiceVO(
                "800101", "2025.8.5技师认定交流会", "千铝工投", "2025.9.10")),
            1,
            10,
            3));

    mockMvc.perform(get("/industrial/talentservice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(3))
        .andExpect(jsonPath("$.data.items[0].title").value("2025.8.5技师认定交流会"))
        .andExpect(jsonPath("$.data.items[0].source").value("千铝工投"))
        .andExpect(jsonPath("$.data.items[0].date").value("2025.9.10"));

    verify(service).pageService(any());
  }
}
