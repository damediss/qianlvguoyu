package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.audit.AuditLog;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class CompetitionNoticeControllerPermissionTest {

  @Test
  void createUsesPublishPermissionAndAudit() {
    Method method = method("create");
    assertThat(method.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:notice:publish')");
    assertAudit(method, "notice:create", false);
  }

  @Test
  void listAndDetailUseViewPermission() {
    assertThat(method("page").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:notice:view')");
    assertThat(method("getById").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:notice:view')");
  }

  @Test
  void publishUsesPublishPermissionAndAuditsTransitionResult() {
    Method method = method("publish");
    assertThat(method.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:notice:publish')");
    assertAudit(method, "notice:publish", true);
  }

  private void assertAudit(Method method, String action, boolean includeResult) {
    AuditLog auditLog = method.getAnnotation(AuditLog.class);
    assertThat(auditLog).isNotNull();
    assertThat(auditLog.module()).isEqualTo("competition");
    assertThat(auditLog.action()).isEqualTo(action);
    assertThat(auditLog.targetType()).isEqualTo("competition_notice");
    assertThat(auditLog.includeResult()).isEqualTo(includeResult);
  }

  private Method method(String name) {
    for (Method method : CompetitionNoticeController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
