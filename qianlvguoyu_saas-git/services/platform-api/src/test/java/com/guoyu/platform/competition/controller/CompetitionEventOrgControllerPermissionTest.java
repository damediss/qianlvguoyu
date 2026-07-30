package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class CompetitionEventOrgControllerPermissionTest {

  @Test
  void controllerSeparatesEventOrganizationViewAndManagePermissions() {
    assertThat(CompetitionEventOrgController.class.getAnnotation(RequestMapping.class).value())
        .containsExactly("/competition/events/{eventId}/organizations");
    assertThat(method("page").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event:view') and hasAuthority('competition:event-org:view')");
    assertThat(method("create").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-org:manage')");
    assertThat(method("updateStatus").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:event-org:manage')");
  }

  private Method method(String name) {
    for (Method method : CompetitionEventOrgController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
