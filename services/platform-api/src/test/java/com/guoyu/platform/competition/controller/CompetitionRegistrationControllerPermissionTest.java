package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.audit.AuditLog;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class CompetitionRegistrationControllerPermissionTest {

  @Test
  void exposesExpectedRegistrationRoutes() {
    assertThat(CompetitionRegistrationController.class.getAnnotation(RequestMapping.class).value())
        .containsExactly("/competition/registrations");
    assertThat(method("page").getAnnotation(GetMapping.class).value()).isEmpty();
    assertThat(method("audit").getAnnotation(PostMapping.class).value())
        .containsExactly("/{registrationId}/audit");
  }

  @Test
  void listUsesRegistrationViewPermission() {
    assertThat(method("page").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:registration:view')");
  }

  @Test
  void auditUsesAuditPermissionAndRecordsTransitionResult() {
    Method audit = method("audit");
    assertThat(audit.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:registration:audit')");
    AuditLog auditLog = audit.getAnnotation(AuditLog.class);
    assertThat(auditLog).isNotNull();
    assertThat(auditLog.module()).isEqualTo("competition");
    assertThat(auditLog.action()).isEqualTo("registration:audit");
    assertThat(auditLog.targetType()).isEqualTo("competition_registration");
    assertThat(auditLog.includeResult()).isTrue();
  }

  private Method method(String name) {
    for (Method method : CompetitionRegistrationController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
