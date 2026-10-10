package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class CompetitionRosterImportPermissionTest {

  @Test
  void rosterRoutesRequireFineGrainedPermissions() {
    assertThat(method("precheck").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:roster:import')");
    assertThat(method("getBatch").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:roster:view')");
    assertThat(method("confirm").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('competition:roster:confirm')");
  }

  private Method method(String name) {
    for (Method method : CompetitionRosterImportController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
