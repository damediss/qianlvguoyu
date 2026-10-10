package com.guoyu.platform.competition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionScoreRuleApplicationService;
import com.guoyu.platform.competition.service.CompetitionScoringReadinessApplicationService;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class CompetitionScoreRuleReadinessControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionScoreRuleApplicationService scoreRuleService;
  private CompetitionScoringReadinessApplicationService readinessService;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    scoreRuleService = context.getBean(CompetitionScoreRuleApplicationService.class);
    readinessService = context.getBean(CompetitionScoringReadinessApplicationService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousScoreRuleManageReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/competition/events/1001/score-rules")
            .contentType("application/json")
            .content(validRuleBody()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(scoreRuleService);
  }

  @Test
  void scoreRuleViewerCannotManageRules() throws Exception {
    mockMvc.perform(post("/competition/events/1001/score-rules")
            .with(user("viewer").authorities(() -> "competition:score-rule:view"))
            .contentType("application/json")
            .content(validRuleBody()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(scoreRuleService);
  }

  @Test
  void anonymousReadinessRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/events/1001/scoring-readiness")
            .param("stageId", "2001")
            .param("eventWorkTypeId", "3001"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(readinessService);
  }

  @Test
  void userWithoutReadinessPermissionReturnsForbidden() throws Exception {
    mockMvc.perform(get("/competition/events/1001/scoring-readiness")
            .with(user("viewer").authorities(() -> "competition:score-rule:view"))
            .param("stageId", "2001")
            .param("eventWorkTypeId", "3001"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(readinessService);
  }

  private String validRuleBody() {
    return """
        {
          "stageId": 2001,
          "eventWorkTypeId": 3001,
          "scoringSystem": "B",
          "theoryTemplate": "THEORY_90",
          "theoryWeight": 40,
          "simulationPracticalWeight": 60
        }
        """;
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionScoreRuleApplicationService scoreRuleService() {
      return Mockito.mock(CompetitionScoreRuleApplicationService.class);
    }

    @Bean
    CompetitionScoringReadinessApplicationService readinessService() {
      return Mockito.mock(CompetitionScoringReadinessApplicationService.class);
    }

    @Bean
    CompetitionScoreRuleController scoreRuleController(
        CompetitionScoreRuleApplicationService service) {
      return new CompetitionScoreRuleController(service);
    }

    @Bean
    CompetitionScoringReadinessController readinessController(
        CompetitionScoringReadinessApplicationService service) {
      return new CompetitionScoringReadinessController(service);
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler() {
      return new GlobalExceptionHandler();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      return http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
          .httpBasic(Customizer.withDefaults())
          .build();
    }
  }
}
