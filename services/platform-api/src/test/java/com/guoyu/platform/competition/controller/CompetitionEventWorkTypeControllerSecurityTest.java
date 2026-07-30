package com.guoyu.platform.competition.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import com.guoyu.platform.competition.service.CompetitionEventWorkTypeApplicationService;
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

class CompetitionEventWorkTypeControllerSecurityTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;
  private CompetitionEventWorkTypeApplicationService service;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(servletContext);
    context.register(TestConfiguration.class);
    context.refresh();
    service = context.getBean(CompetitionEventWorkTypeApplicationService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  void anonymousManageRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/competition/events/200/work-types")
            .contentType("application/json")
            .content(validBody()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(service);
  }

  @Test
  void viewerCannotManageEventWorkTypes() throws Exception {
    mockMvc.perform(post("/competition/events/200/work-types")
            .with(user("viewer")
                .authorities(() -> "competition:event-work-type:view"))
            .contentType("application/json")
            .content(validBody()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  private String validBody() {
    return """
        {"workTypeId":100,"displayName":"焊工","status":"active","sortOrder":1}
        """;
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @EnableMethodSecurity
  static class TestConfiguration {

    @Bean
    CompetitionEventWorkTypeApplicationService eventWorkTypeService() {
      return Mockito.mock(CompetitionEventWorkTypeApplicationService.class);
    }

    @Bean
    CompetitionEventWorkTypeController eventWorkTypeController(
        CompetitionEventWorkTypeApplicationService service) {
      return new CompetitionEventWorkTypeController(service);
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
