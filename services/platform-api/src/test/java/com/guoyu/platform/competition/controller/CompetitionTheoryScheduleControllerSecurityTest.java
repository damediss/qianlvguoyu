package com.guoyu.platform.competition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionTheoryRosterExportService;
import com.guoyu.platform.competition.service.CompetitionTheoryScheduleApplicationService;
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

class CompetitionTheoryScheduleControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionTheoryScheduleApplicationService service;
  private CompetitionTheoryRosterExportService exportService;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    service = context.getBean(CompetitionTheoryScheduleApplicationService.class);
    exportService = context.getBean(CompetitionTheoryRosterExportService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousScheduleQueryReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/events/1001/theory-schedules"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(service);
  }

  @Test
  void scheduleViewerCannotPublishOrExport() throws Exception {
    var viewer = user("viewer").authorities(() -> "competition:theory-schedule:view");
    mockMvc.perform(post("/competition/theory-schedules/2001/publish").with(viewer))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/competition/theory-schedules/2001/roster.xlsx").with(viewer))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service, exportService);
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionTheoryScheduleApplicationService scheduleService() {
      return Mockito.mock(CompetitionTheoryScheduleApplicationService.class);
    }

    @Bean
    CompetitionTheoryRosterExportService exportService() {
      return Mockito.mock(CompetitionTheoryRosterExportService.class);
    }

    @Bean
    CompetitionTheoryScheduleController scheduleController(
        CompetitionTheoryScheduleApplicationService service,
        CompetitionTheoryRosterExportService exportService) {
      return new CompetitionTheoryScheduleController(service, exportService);
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
