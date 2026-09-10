package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentBatchReq;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionStagePaperAssignmentPermissionTest {
  @Configuration @EnableMethodSecurity
  static class Config {
    @Bean CompetitionStagePaperAssignmentApplicationService service() {
      return mock(CompetitionStagePaperAssignmentApplicationService.class);
    }
    @Bean CompetitionStagePaperAssignmentController controller(CompetitionStagePaperAssignmentApplicationService service) {
      return new CompetitionStagePaperAssignmentController(service);
    }
  }

  @Test void viewOnlyCannotAllocateAndConfirmOnlyCannotWriteAssignments() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionStagePaperAssignmentController.class);
      var service = context.getBean(CompetitionStagePaperAssignmentApplicationService.class);
      authenticate("view");
      controller.summary(10L);
      verify(service).summary(10L);
      assertThatThrownBy(() -> controller.autoAllocate(10L)).isInstanceOf(AccessDeniedException.class);
      authenticate("confirm");
      assertThatThrownBy(() -> controller.confirm(10L, new CompetitionStagePaperAssignmentBatchReq(List.of())))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).autoAllocate(anyLong());
      verify(service, never()).confirm(anyLong(), any());
    } finally { SecurityContextHolder.clearContext(); }
  }

  private void authenticate(String action) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "test", "", List.of(new SimpleGrantedAuthority("competition:stage-paper-assignment:" + action))));
  }
}
