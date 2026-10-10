package com.guoyu.platform.competition;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompetitionStageLifecycleActionHttpTest {

  private MockMvc mockMvc;
  private CompetitionStageApplicationService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(CompetitionStageApplicationService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(
        new com.guoyu.platform.competition.controller.CompetitionStageController(service)).build();
  }

  @Test
  void lifecycleUsesNamedDomainActionsAndHasNoTargetStatusRoute() throws Exception {
    mockMvc.perform(post("/competition/stages/10/configuration")).andExpect(status().isOk());
    mockMvc.perform(post("/competition/stages/10/actions/lock-ready")).andExpect(status().isOk());
    mockMvc.perform(post("/competition/stages/10/actions/start")).andExpect(status().isOk());
    mockMvc.perform(post("/competition/stages/10/actions/close")).andExpect(status().isOk());
    mockMvc.perform(post("/competition/stages/10/actions/mark-settled")).andExpect(status().isOk());
    mockMvc.perform(post("/competition/stages/10/actions/publish-result")).andExpect(status().isOk());

    verify(service).beginConfiguration(10L, null);
    verify(service).lockReady(10L);
    verify(service).startStage(10L);
    verify(service).closeStage(10L);
    verify(service).markSettled(10L);
    verify(service).publishResult(10L);
    org.assertj.core.api.Assertions.assertThat(
        java.util.Arrays.stream(com.guoyu.platform.competition.controller.CompetitionStageController.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName))
        .doesNotContain("transition", "lifecycle");
  }
}
