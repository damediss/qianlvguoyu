package com.guoyu.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(OutputCaptureExtension.class)
class RestSecurityRejectionLoggingTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void authenticationEntryPointReturns401AndLogsSafeRequestContext(CapturedOutput output)
      throws Exception {
    MockHttpServletRequest request = request("GET", "/competition/registrations");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAuthenticationEntryPoint(objectMapper).commence(
        request, response, new BadCredentialsException("sensitive credential detail"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("\"code\":\"401\"");
    assertThat(output).contains(
        "Unauthenticated request rejected, method=GET, uri=/competition/registrations");
    assertThat(output).doesNotContain("sensitive credential detail");
  }

  @Test
  void accessDeniedHandlerReturns403AndLogsSafeRequestContext(CapturedOutput output)
      throws Exception {
    MockHttpServletRequest request = request(
        "POST", "/competition/registrations/9101/audit");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAccessDeniedHandler(objectMapper).handle(
        request, response, new AccessDeniedException("sensitive authorization detail"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"code\":\"403\"");
    assertThat(output).contains(
        "Authorized request denied by access control, method=POST, uri=/competition/registrations/9101/audit");
    assertThat(output).doesNotContain("sensitive authorization detail");
  }

  @Test
  void methodAuthorizationHandlerReturns403AndLogsRejection(CapturedOutput output) {
    AuthorizationDeniedException exception = Mockito.mock(AuthorizationDeniedException.class);

    var response = new GlobalExceptionHandler().handleAccessDenied(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("403");
    assertThat(output).contains(
        "Request denied by method authorization, exceptionType=AuthorizationDeniedException");
  }

  private MockHttpServletRequest request(String method, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(method);
    request.setRequestURI(uri);
    return request;
  }
}
