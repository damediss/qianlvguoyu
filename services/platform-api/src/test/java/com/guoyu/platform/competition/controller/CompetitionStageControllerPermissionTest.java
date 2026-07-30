package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.audit.AuditLog;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class CompetitionStageControllerPermissionTest {

  @Test
  void exposesExpectedStageRoutes() {
    assertThat(CompetitionStageController.class.getAnnotation(RequestMapping.class).value())
        .containsExactly("/competition");
    assertThat(method("page").getAnnotation(GetMapping.class).value())
        .containsExactly("/events/{eventId}/stages");
    assertThat(method("create").getAnnotation(PostMapping.class).value())
        .containsExactly("/events/{eventId}/stages");
    assertThat(method("update").getAnnotation(PutMapping.class).value())
        .containsExactly("/stages/{stageId}");
  }

  @Test
  void viewUsesStageViewPermission() {
    assertThat(method("page").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:stage:view')");
  }

  @Test
  void writesUseManagePermissionAndAuditLog() {
    Method create = method("create");
    assertThat(create.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:stage:manage')");
    assertAudit(create, "stage:create");

    Method update = method("update");
    assertThat(update.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:stage:manage')");
    assertAudit(update, "stage:update");
  }

  private void assertAudit(Method method, String action) {
    AuditLog auditLog = method.getAnnotation(AuditLog.class);
    assertThat(auditLog).isNotNull();
    assertThat(auditLog.module()).isEqualTo("competition");
    assertThat(auditLog.action()).isEqualTo(action);
    assertThat(auditLog.targetType()).isEqualTo("competition_stage");
  }

  private Method method(String name) {
    for (Method method : CompetitionStageController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
