package com.guoyu.platform.industrial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.industrial.dto.IndustrialPlatformQueryReq;
import com.guoyu.platform.industrial.service.IndustrialPlatformApplicationService;
import com.guoyu.platform.industrial.vo.IndustrialPlatformItemVO;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IndustrialPlatformControllerApiTest {

  private MockMvc mockMvc;

  private IndustrialPlatformApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(IndustrialPlatformApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new IndustrialPlatformController(service))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void getMasterReturnsUnifiedPageResponseAndPassesQueryParameters() throws Exception {
    IndustrialPlatformItemVO item = new IndustrialPlatformItemVO(
        "520001", "master", "Guoyu Aluminium", "Master Studio", 12, null, null, 1L,
        LocalDateTime.of(2026, 5, 27, 9, 0), LocalDateTime.of(2026, 5, 27, 9, 30));
    when(service.page(eq("master"), any(IndustrialPlatformQueryReq.class)))
        .thenReturn(new PageResponse<>(List.of(item), 1, 10, 1));

    mockMvc.perform(get("/industrial/platform-master")
            .param("keyword", "Guoyu")
            .param("company", "Aluminium")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.message").value("OK"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value("520001"))
        .andExpect(jsonPath("$.data.items[0].category").value("master"))
        .andExpect(jsonPath("$.data.items[0].company").value("Guoyu Aluminium"));

    ArgumentCaptor<IndustrialPlatformQueryReq> captor = ArgumentCaptor.forClass(IndustrialPlatformQueryReq.class);
    verify(service).page(eq("master"), captor.capture());
    IndustrialPlatformQueryReq req = captor.getValue();
    Assertions.assertThat(req.getKeyword()).isEqualTo("Guoyu");
    Assertions.assertThat(req.getCompany()).isEqualTo("Aluminium");
    Assertions.assertThat(req.getPage()).isEqualTo(1);
    Assertions.assertThat(req.getPageSize()).isEqualTo(10);
  }

  @Test
  void createMasterRejectsMissingRequiredCompanyBeforeCallingService() throws Exception {
    mockMvc.perform(post("/industrial/platform-master")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Master Studio",
                  "nums": 12
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.message").exists());

    verifyNoInteractions(service);
  }
}
