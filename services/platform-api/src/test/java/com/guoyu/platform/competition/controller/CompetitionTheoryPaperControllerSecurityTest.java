package com.guoyu.platform.competition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperApplicationService;
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
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class CompetitionTheoryPaperControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionTheoryPaperApplicationService service;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    service = context.getBean(CompetitionTheoryPaperApplicationService.class);
    mockMvc = webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousRequirementQueryReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/score-rules/1001/theory-requirement"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(service);
  }

  @Test
  void viewerCannotImportPaperSnapshots() throws Exception {
    mockMvc.perform(post("/competition/events/1001/theory-exam/paper-snapshots/import")
            .with(user("viewer").authorities(() -> "competition:theory-paper:view"))
            .contentType("application/json")
            .content("""
                {
                  "scoreRuleId": 2001,
                  "courseId": 2501,
                  "paperAId": 3001,
                  "paperBId": 3002
                }
                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionTheoryPaperApplicationService theoryPaperService() {
      return Mockito.mock(CompetitionTheoryPaperApplicationService.class);
    }

    @Bean
    CompetitionTheoryPaperController theoryPaperController(
        CompetitionTheoryPaperApplicationService service) {
      return new CompetitionTheoryPaperController(service);
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
