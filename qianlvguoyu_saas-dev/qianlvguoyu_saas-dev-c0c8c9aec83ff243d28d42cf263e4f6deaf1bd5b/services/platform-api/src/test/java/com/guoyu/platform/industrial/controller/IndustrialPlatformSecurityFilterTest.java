package com.guoyu.platform.industrial.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.security.RestAccessDeniedHandler;
import com.guoyu.platform.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class IndustrialPlatformSecurityFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void authenticationEntryPointReturnsUnifiedUnauthorizedResponse() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAuthenticationEntryPoint(objectMapper).commence(
        new MockHttpServletRequest(),
        response,
        new InsufficientAuthenticationException("missing token")
    );

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(body.get("code").asText()).isEqualTo("401");
    assertThat(body.get("message").asText()).isEqualTo("Authentication required");
  }

  @Test
  void accessDeniedHandlerReturnsUnifiedForbiddenResponse() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAccessDeniedHandler(objectMapper).handle(
        new MockHttpServletRequest(),
        response,
        new AccessDeniedException("no permission")
    );

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(body.get("code").asText()).isEqualTo("403");
    assertThat(body.get("message").asText()).isEqualTo("Access denied");
  }
}
