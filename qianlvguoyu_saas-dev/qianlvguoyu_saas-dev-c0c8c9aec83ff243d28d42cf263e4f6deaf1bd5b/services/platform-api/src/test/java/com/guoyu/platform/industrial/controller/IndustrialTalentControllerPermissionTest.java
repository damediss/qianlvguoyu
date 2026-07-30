package com.guoyu.platform.industrial.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class IndustrialTalentControllerPermissionTest {

  @Test
  void controllerExposesLegacyTalentRoutes() {
    RequestMapping mapping = IndustrialTalentController.class.getAnnotation(RequestMapping.class);

    assertThat(mapping.value()).containsExactly("/industrial");
    assertThat(method("talentTraining").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/talenttraining");
    assertThat(method("talentService").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/talentservice");
  }

  @Test
  void controllerUsesSeparatePermissionCodesPerTalentMenu() {
    assertThat(method("talentTraining").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:talent-training:view')");
    assertThat(method("talentService").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:talent-service:view')");
  }

  private Method method(String name) {
    for (Method method : IndustrialTalentController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
