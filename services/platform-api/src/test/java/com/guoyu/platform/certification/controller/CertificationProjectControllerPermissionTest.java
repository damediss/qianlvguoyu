package com.guoyu.platform.certification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class CertificationProjectControllerPermissionTest {

  @Test
  void controllerExposesCertificationProjectRoutes() {
    RequestMapping mapping = CertificationProjectController.class.getAnnotation(RequestMapping.class);

    assertThat(mapping.value()).containsExactly("/certification/projects");
    assertThat(method("pageTasks").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/tasks");
    assertThat(method("workbench").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/tasks/{taskId}/workbench");
    assertThat(method("submitDocument").getAnnotation(org.springframework.web.bind.annotation.PostMapping.class).value())
        .containsExactly("/tasks/{taskId}/documents/{documentType}/submit");
    assertThat(method("initialReviewDocument").getAnnotation(org.springframework.web.bind.annotation.PostMapping.class).value())
        .containsExactly("/tasks/{taskId}/documents/{documentType}/initial-review");
    assertThat(method("finalReviewDocument").getAnnotation(org.springframework.web.bind.annotation.PostMapping.class).value())
        .containsExactly("/tasks/{taskId}/documents/{documentType}/final-review");
    assertThat(method("getDocumentWorkflow").getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
        .containsExactly("/tasks/{taskId}/documents/{documentType}/workflow");
    assertThat(method("toggleRiskStatus").getAnnotation(org.springframework.web.bind.annotation.PutMapping.class).value())
        .containsExactly("/tasks/{taskId}/risks/{riskId}/status");
  }

  @Test
  void controllerUsesFineGrainedCertificationPermissions() {
    assertThat(method("pageTasks").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.task.list')");
    assertThat(method("createTask").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.task.create')");
    assertThat(method("flow").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.task.flow')");
    assertThat(method("saveMembers").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.members.edit')");
    assertThat(method("submitDocument").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.document.submit')");
    assertThat(method("initialReviewDocument").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.document.initialReview')");
    assertThat(method("finalReviewDocument").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.document.finalReview')");
    assertThat(method("getDocumentWorkflow").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.document.workflowView')");
    assertThat(method("saveRisk").getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('certification.project.risk.edit')");
  }

  private Method method(String name) {
    for (Method method : CertificationProjectController.class.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + name);
  }
}
