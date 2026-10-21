package com.guoyu.platform.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionScoringApplicationService;
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

class CompetitionScoringControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionScoringApplicationService service;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    service = context.getBean(CompetitionScoringApplicationService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousScoreSourceListReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/events/1001/score-sources")
            .param("stageId", "2001")
            .param("eventWorkTypeId", "3001"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void scoreViewerCannotReviseSource() throws Exception {
    mockMvc.perform(post("/competition/events/1001/score-sources/revisions")
            .with(user("viewer").authorities(() -> "competition:score:view"))
            .contentType("application/json")
            .content(validRevisionBody()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void scoreManagerCannotCalculateWithoutDedicatedPermission() throws Exception {
    mockMvc.perform(post("/competition/events/1001/total-score-snapshots/calculate")
            .with(user("manager").authorities(() -> "competition:score:manage"))
            .contentType("application/json")
            .content("""
                {"stageId":2001,"eventWorkTypeId":3001,
                 "contestantMemberId":4001,"scoreRuleId":5001}
                """))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void rankingViewerCannotGenerateSnapshot() throws Exception {
    mockMvc.perform(post("/competition/events/1001/ranking-snapshots/generate")
            .with(user("viewer").authorities(() -> "competition:ranking:view"))
            .contentType("application/json")
            .content("""
                {"stageId":2001,"eventWorkTypeId":3001,"scoreRuleId":5001,
                 "candidates":[{"contestantMemberId":4001,
                 "totalScoreSnapshotId":6001,"tieKey":"90|80",
                 "contestantStatus":"completed"}]}
                """))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void calculatedRankingPublishRequiresReviewAndPublishPermissions() throws Exception {
    String body = """
        {"stageId":2001,"eventWorkTypeId":3001}
        """;

    mockMvc.perform(post("/competition/events/1001/ranking-snapshots/publish-calculated")
            .with(user("publisher").authorities(() -> "competition:ranking:publish"))
            .contentType("application/json")
            .content(body))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/competition/events/1001/ranking-snapshots/publish-calculated")
            .with(user("reviewer").authorities(() -> "competition:score:review"))
            .contentType("application/json")
            .content(body))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  @Test
  void calculatedRankingPublishAcceptsUserWithBothPermissions() throws Exception {
    mockMvc.perform(post("/competition/events/1001/ranking-snapshots/publish-calculated")
            .with(user("reviewer-publisher").authorities(
                () -> "competition:ranking:publish", () -> "competition:score:review"))
            .contentType("application/json")
            .content("""
                {"stageId":2001,"eventWorkTypeId":3001}
                """))
        .andExpect(status().isOk());

    verify(service).publishCalculatedRanking(eq(1001L), any());
  }

  @Test
  void scoreViewerCannotReadRankingEntries() throws Exception {
    mockMvc.perform(get("/competition/ranking-snapshots/7001/entries")
            .with(user("viewer").authorities(() -> "competition:score:view")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void nullRankingCandidatesReturnsBadRequestWithoutCallingService() throws Exception {
    mockMvc.perform(post("/competition/events/1001/ranking-snapshots/generate")
            .with(user("generator").authorities(() -> "competition:ranking:generate"))
            .contentType("application/json")
            .content("""
                {"stageId":2001,"eventWorkTypeId":3001,
                 "scoreRuleId":5001,"candidates":null}
                """))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  private String validRevisionBody() {
    return """
        {"stageId":2001,"eventWorkTypeId":3001,"contestantMemberId":4001,
         "sourceType":"theory","rawScore":88,"confirmationStatus":"confirmed",
         "sourceSystem":"competition","sourceReference":"manual-1",
         "scoreRuleId":5001,"achievedAt":"2026-07-31T10:00:00"}
        """;
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionScoringApplicationService scoringService() {
      return Mockito.mock(CompetitionScoringApplicationService.class);
    }

    @Bean
    CompetitionScoringController scoringController(
        CompetitionScoringApplicationService service) {
      return new CompetitionScoringController(service);
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
