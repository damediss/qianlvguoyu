package com.guoyu.platform.industrial.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class IndustrialPlatformControllerPermissionTest {

  @Test
  void controllerExposesRequiredHyphenatedPlatformRoutes() {
    RequestMapping mapping = IndustrialPlatformController.class.getAnnotation(RequestMapping.class);

    assertThat(mapping.value()).containsExactly("/industrial");
    assertThat(method("pageMaster").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/platform-master");
    assertThat(method("pageStudy").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/platform-study");
    assertThat(method("pageMatch").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/platform-match");
    assertThat(method("pageApprentice").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/platform-apprentice");
    assertThat(method("pageComment").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/platform-comment");
  }

  @Test
  void controllerUsesSeparatePermissionCodesPerPlatformMenu() {
    assertThat(method("pageMaster").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:platform-master:view')");
    assertThat(method("createStudy").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:platform-study:create')");
    assertThat(method("updateMatch").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:platform-match:update')");
    assertThat(method("deleteApprentice").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:platform-apprentice:delete')");
    assertThat(method("pageComment").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('industrial:platform-comment:view')");
  }

  private Method method(String name) {
    for (Method method : IndustrialPlatformController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
