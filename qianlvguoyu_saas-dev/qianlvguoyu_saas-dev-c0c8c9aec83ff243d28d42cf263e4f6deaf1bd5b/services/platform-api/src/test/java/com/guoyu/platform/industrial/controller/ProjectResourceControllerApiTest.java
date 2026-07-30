package com.guoyu.platform.industrial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.industrial.dto.ProjectResourceQueryReq;
import com.guoyu.platform.industrial.service.ProjectResourceApplicationService;
import com.guoyu.platform.industrial.vo.ProjectResourceVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProjectResourceControllerApiTest {

  private MockMvc mockMvc;
  private ProjectResourceApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(ProjectResourceApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new ProjectResourceController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void projectCourseEndpointReturnsUnifiedPageWithSnakeCaseFileType() throws Exception {
    ProjectResourceVO item = new ProjectResourceVO(
        "course-1", "course", "2025-06-10", "铝电解槽操作工高级技师",
        "通过", "千铝工投", null, "https://cdn.example.com/a.pptx", "pptx");
    when(service.page(eq("course"), any(ProjectResourceQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(item), 1, 10, 1));

    mockMvc.perform(get("/industrial/projectcourse")
            .param("keyword", "高级技师")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].name").value("铝电解槽操作工高级技师"))
        .andExpect(jsonPath("$.data.items[0].file_type").value("pptx"));

    ArgumentCaptor<ProjectResourceQueryReq> captor = ArgumentCaptor.forClass(ProjectResourceQueryReq.class);
    verify(service).page(eq("course"), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getKeyword()).isEqualTo("高级技师");
  }

  @Test
  void projectTrainEndpointUsesTrainResourceType() throws Exception {
    when(service.page(eq("train"), any(ProjectResourceQueryReq.class)))
        .thenReturn(PageResponse.empty(1, 20));

    mockMvc.perform(get("/industrial/projecttrain"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"));

    verify(service).page(eq("train"), any(ProjectResourceQueryReq.class));
  }
}
