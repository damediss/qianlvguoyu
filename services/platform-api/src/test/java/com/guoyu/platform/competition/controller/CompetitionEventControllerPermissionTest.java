package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.audit.AuditLog;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class CompetitionEventControllerPermissionTest {

  @Test
  void publishRequiresPublishPermissionAndAuditLog() {
    Method publish = method("publish");
    assertThat(publish.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event:publish')");
    AuditLog auditLog = publish.getAnnotation(AuditLog.class);
    assertThat(auditLog).isNotNull();
    assertThat(auditLog.module()).isEqualTo("competition");
    assertThat(auditLog.action()).isEqualTo("event:publish");
    assertThat(auditLog.targetType()).isEqualTo("competition_event");
    assertThat(auditLog.includeResult()).isTrue();
  }

  @Test
  void publishReadinessRequiresPublishPermission() {
    Method readiness = method("publishReadiness");
    assertThat(readiness.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event:publish')");
  }

  private Method method(String name) {
    for (Method method : CompetitionEventController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
