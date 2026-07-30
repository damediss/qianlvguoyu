package com.guoyu.platform.certification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.certification.dto.CertificationTaskQueryReq;
import com.guoyu.platform.certification.service.CertificationProjectApplicationService;
import com.guoyu.platform.certification.vo.CertificationTaskScopeVO;
import com.guoyu.platform.certification.vo.CertificationTaskVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CertificationProjectControllerApiTest {

  private MockMvc mockMvc;

  private CertificationProjectApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CertificationProjectApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new CertificationProjectController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void pageTasksReturnsUnifiedPageResponseAndPassesFilters() throws Exception {
    CertificationTaskVO task = new CertificationTaskVO(
        "9001",
        "RD-2026-001",
        "2026 年职业技能认定任务",
        "张晓峰",
        "preparation",
        "任务准备阶段",
        "running",
        "进行中",
        "manual",
        "手动新增",
        "电工",
        "senior",
        "高级",
        List.<CertificationTaskScopeVO>of(),
        60,
        LocalDate.of(2026, 5, 8),
        LocalDate.of(2026, 5, 20),
        LocalDate.of(2026, 5, 20),
        1,
        5,
        1,
        5,
        LocalDateTime.of(2026, 5, 29, 10, 0)
    );
    when(service.pageTasks(any(CertificationTaskQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(task), 1, 10, 1));

    mockMvc.perform(get("/certification/projects/tasks")
            .param("keyword", "职业技能")
            .param("status", "running")
            .param("levelCode", "senior")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.message").value("OK"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].taskNo").value("RD-2026-001"))
        .andExpect(jsonPath("$.data.items[0].taskName").value("2026 年职业技能认定任务"));

    ArgumentCaptor<CertificationTaskQueryReq> captor = ArgumentCaptor.forClass(CertificationTaskQueryReq.class);
    verify(service).pageTasks(captor.capture());
    CertificationTaskQueryReq req = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(req.getKeyword()).isEqualTo("职业技能");
    org.assertj.core.api.Assertions.assertThat(req.getStatus()).isEqualTo("running");
    org.assertj.core.api.Assertions.assertThat(req.getLevelCode()).isEqualTo("senior");
    org.assertj.core.api.Assertions.assertThat(req.getPage()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(req.getPageSize()).isEqualTo(10);
  }

  @Test
  void createTaskRejectsMissingRequiredTaskNoBeforeCallingService() throws Exception {
    mockMvc.perform(post("/certification/projects/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "2026 年职业技能认定任务",
                  "taskManagerName": "张晓峰"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.message").exists());

    verifyNoInteractions(service);
  }
}
