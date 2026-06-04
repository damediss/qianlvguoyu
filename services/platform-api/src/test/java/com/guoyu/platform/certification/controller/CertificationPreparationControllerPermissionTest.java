package com.guoyu.platform.certification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class CertificationPreparationControllerPermissionTest {

  @Test
  void preparationControllersExposePromptRoutes() {
    assertThat(mapping(CertificationPreparationController.class)).containsExactly("/certification/preparation");
    assertThat(get(CertificationPreparationController.class, "pageTasks"))
        .containsExactly("/tasks");
    assertThat(get(CertificationPreparationController.class, "overview"))
        .containsExactly("/tasks/{taskId}/overview");
    assertThat(post(CertificationPreparationController.class, "complete"))
        .containsExactly("/tasks/{taskId}/complete");

    assertThat(mapping(CertificationPreparationCandidateController.class)).containsExactly("/certification/preparation");
    assertThat(post(CertificationPreparationCandidateController.class, "createImportBatch"))
        .containsExactly("/tasks/{taskId}/candidate-imports");
    assertThat(get(CertificationPreparationCandidateController.class, "pageCandidates"))
        .containsExactly("/tasks/{taskId}/candidates");
    assertThat(delete(CertificationPreparationCandidateController.class, "deleteCandidate"))
        .containsExactly("/tasks/{taskId}/candidates/{candidateId}");

    assertThat(mapping(CertificationAssessorWorkbenchController.class)).containsExactly("/certification/preparation");
    assertThat(get(CertificationAssessorWorkbenchController.class, "pageTasks"))
        .containsExactly("/assessor-workbench/tasks");
    assertThat(put(CertificationAssessorWorkbenchController.class, "saveDraft"))
        .containsExactly("/assessor-workbench/tasks/{taskId}/draft");
    assertThat(post(CertificationAssessorWorkbenchController.class, "review"))
        .containsExactly("/assessor-workbench/tasks/{taskId}/review");
  }

  @Test
  void preparationControllersUseFineGrainedPermissions() {
    assertThat(preAuthorize(CertificationPreparationController.class, "pageTasks"))
        .isEqualTo("hasAuthority('certification.preparation.task.view')");
    assertThat(preAuthorize(CertificationPreparationCandidateController.class, "createCandidate"))
        .isEqualTo("hasAuthority('certification.preparation.candidate.edit')");
    assertThat(preAuthorize(CertificationPreparationCandidateController.class, "deleteCandidate"))
        .isEqualTo("hasAuthority('certification.preparation.candidate.delete')");
    assertThat(preAuthorize(CertificationPreparationDocumentController.class, "reviewCandidateDocument"))
        .isEqualTo("hasAuthority('certification.preparation.document.review')");
    assertThat(preAuthorize(CertificationPreparationMaterialController.class, "generateMaterials"))
        .isEqualTo("hasAuthority('certification.preparation.material.generate')");
    assertThat(preAuthorize(CertificationPreparationSiteController.class, "assignExamSite"))
        .isEqualTo("hasAuthority('certification.preparation.site.assign')");
    assertThat(preAuthorize(CertificationPreparationAssessorController.class, "dispatchGroup"))
        .isEqualTo("hasAuthority('certification.preparation.assessor.dispatch')");
    assertThat(preAuthorize(CertificationAssessorWorkbenchController.class, "saveDraft"))
        .isEqualTo("hasAuthority('certification.preparation.assessor.score')");
    assertThat(preAuthorize(CertificationAssessorWorkbenchController.class, "review"))
        .isEqualTo("hasAuthority('certification.preparation.assessor.review')");
    assertThat(preAuthorize(CertificationPreparationController.class, "complete"))
        .isEqualTo("hasAuthority('certification.preparation.complete')");
  }

  private String[] mapping(Class<?> controller) {
    return controller.getAnnotation(RequestMapping.class).value();
  }

  private String preAuthorize(Class<?> controller, String name) {
    return method(controller, name).getAnnotation(PreAuthorize.class).value();
  }

  private String[] get(Class<?> controller, String name) {
    return method(controller, name).getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value();
  }

  private String[] post(Class<?> controller, String name) {
    return method(controller, name).getAnnotation(org.springframework.web.bind.annotation.PostMapping.class).value();
  }

  private String[] put(Class<?> controller, String name) {
    return method(controller, name).getAnnotation(org.springframework.web.bind.annotation.PutMapping.class).value();
  }

  private String[] delete(Class<?> controller, String name) {
    return method(controller, name).getAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class).value();
  }

  private Method method(Class<?> controller, String name) {
    for (Method method : controller.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Missing method " + controller.getSimpleName() + "." + name);
  }
}
