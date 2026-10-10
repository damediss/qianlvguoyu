package com.guoyu.platform.competition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionRegistrationApplicationService;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
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

class CompetitionStageRegistrationControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionStageApplicationService stageService;
  private CompetitionRegistrationApplicationService registrationService;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    stageService = context.getBean(CompetitionStageApplicationService.class);
    registrationService = context.getBean(CompetitionRegistrationApplicationService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousStageRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/events/1001/stages"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(stageService);
  }

  @Test
  void stageWriteWithoutManagePermissionReturnsForbidden() throws Exception {
    mockMvc.perform(post("/competition/events/1001/stages")
            .with(user("viewer").authorities(() -> "competition:stage:view"))
            .contentType("application/json")
            .content("""
                {
                  "name": "Theory",
                  "stageType": "theory",
                  "sortOrder": 1
                }
                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(stageService);
  }

  @Test
  void anonymousRegistrationRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/competition/registrations"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(registrationService);
  }

  @Test
  void registrationAuditWithoutAuditPermissionReturnsForbidden() throws Exception {
    mockMvc.perform(post("/competition/registrations/9101/audit")
            .with(user("viewer").authorities(() -> "competition:registration:view"))
            .contentType("application/json")
            .content("{\"action\":\"approve\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(registrationService);
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionStageApplicationService stageService() {
      return Mockito.mock(CompetitionStageApplicationService.class);
    }

    @Bean
    CompetitionRegistrationApplicationService registrationService() {
      return Mockito.mock(CompetitionRegistrationApplicationService.class);
    }

    @Bean
    CompetitionStageController stageController(CompetitionStageApplicationService service) {
      return new CompetitionStageController(service);
    }

    @Bean
    CompetitionRegistrationController registrationController(
        CompetitionRegistrationApplicationService service) {
      return new CompetitionRegistrationController(service);
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
