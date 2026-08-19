package com.guoyu.platform.certification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class CertificationDefenseScoringControllerPermissionTest {

  @Test
  void controllerExposesAdminAndAssessorRoutes() {
    assertThat(mapping(CertificationDefenseScoringController.class)).containsExactly("/certification");
    assertThat(get("setup")).containsExactly("/defense-scoring/tasks/{taskId}/setup");
    assertThat(put("saveConfig")).containsExactly("/defense-scoring/tasks/{taskId}/config");
    assertThat(post("matchReports")).containsExactly("/defense-scoring/tasks/{taskId}/reports/match");
    assertThat(get("myReports")).containsExactly("/assessor-defense/reports");
    assertThat(get("myReport")).containsExactly("/assessor-defense/reports/{reportId}");
  }

  @Test
  void controllerUsesFineGrainedPermissions() {
    assertThat(preAuthorize("setup"))
        .isEqualTo("hasAnyAuthority('certification.defense.config', 'certification.preparation.task.view')");
    assertThat(preAuthorize("matchReports"))
        .isEqualTo("hasAnyAuthority('certification.defense.upload', 'certification.preparation.task.view')");
    assertThat(preAuthorize("myReports")).isEqualTo("hasAuthority('certification.defense.review')");
    assertThat(preAuthorize("myReport")).isEqualTo("hasAuthority('certification.defense.review')");
  }

  private String[] mapping(Class<?> controller) {
    return controller.getAnnotation(RequestMapping.class).value();
  }

  private String preAuthorize(String name) {
    return method(name).getAnnotation(PreAuthorize.class).value();
  }

  private String[] get(String name) {
    return method(name).getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value();
  }

  private String[] post(String name) {
    return method(name).getAnnotation(org.springframework.web.bind.annotation.PostMapping.class).value();
  }

  private String[] put(String name) {
    return method(name).getAnnotation(org.springframework.web.bind.annotation.PutMapping.class).value();
  }

  private Method method(String name) {
    for (Method method : CertificationDefenseScoringController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new IllegalArgumentException(name);
  }
}
