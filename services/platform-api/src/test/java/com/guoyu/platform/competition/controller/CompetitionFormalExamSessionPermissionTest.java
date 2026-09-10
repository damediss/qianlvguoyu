package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.guoyu.platform.competition.dto.CompetitionFormalCandidateExamQueryReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamForceSubmitReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamSessionStartReq;
import com.guoyu.platform.competition.exam.realtime.FormalExamConsoleRealtimeApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamSessionApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamScoringService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionFormalExamSessionPermissionTest {

  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean
    CompetitionFormalExamSessionApplicationService service() {
      return mock(CompetitionFormalExamSessionApplicationService.class);
    }

    @Bean
    CompetitionFormalExamScoringService scoringService() {
      return mock(CompetitionFormalExamScoringService.class);
    }

    @Bean
    FormalExamConsoleRealtimeApplicationService consoleRealtime() {
      return mock(FormalExamConsoleRealtimeApplicationService.class);
    }

    @Bean
    CompetitionFormalExamSessionController controller(
        CompetitionFormalExamSessionApplicationService service,
        CompetitionFormalExamScoringService scoringService,
        FormalExamConsoleRealtimeApplicationService consoleRealtime) {
      return new CompetitionFormalExamSessionController(service, scoringService, consoleRealtime);
    }
  }

  @Test
  void viewOnlyCannotPrepare() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var service = context.getBean(CompetitionFormalExamSessionApplicationService.class);
      authenticate("competition:exam-session:view");
      controller.byStage(10L, "theory");
      controller.preflight(9L);
      controller.candidate(8L);
      verify(service).getByStage(10L, "theory");
      verify(service).getPreflight(9L);
      verify(service).getCandidate(8L);
      assertThatThrownBy(() -> controller.prepare(10L, "theory"))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.start(9L, new CompetitionFormalExamSessionStartReq(true)))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).prepareExamSession(10L, "theory");
      verify(service, never()).startExamSession(any(), any());
      assertThatThrownBy(() -> controller.forceSubmit(
          9L, 8L, new CompetitionFormalExamForceSubmitReq("现场设备异常")))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).forceSubmit(any(), any(), any());
      assertThatThrownBy(() -> controller.forceOffline(9L, 8L))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).forceOffline(any(), any());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void scoreCannotReadWithoutScoreAuthority() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var scoring = context.getBean(CompetitionFormalExamScoringService.class);
      authenticate("competition:exam-session:view");
      assertThatThrownBy(() -> controller.score(9L)).isInstanceOf(AccessDeniedException.class);
      verify(scoring, never()).retryFromAdmin(any());
      controller.candidateResult(8L);
      verify(context.getBean(CompetitionFormalExamSessionApplicationService.class))
          .getCandidateResult(8L);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void scoreAuthorityCannotReadWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var scoring = context.getBean(CompetitionFormalExamScoringService.class);
      authenticate("competition:exam-session:score");
      controller.score(9L);
      verify(scoring).retryFromAdmin(9L);
      assertThatThrownBy(() -> controller.get(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.candidateResult(8L))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void startCannotReadWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var service = context.getBean(CompetitionFormalExamSessionApplicationService.class);
      authenticate("competition:exam-session:start");
      controller.start(9L, new CompetitionFormalExamSessionStartReq(true));
      verify(service).startExamSession(9L, new CompetitionFormalExamSessionStartReq(true));
      assertThatThrownBy(() -> controller.get(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.preflight(9L)).isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void prepareCannotReadWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      authenticate("competition:exam-session:prepare");
      controller.prepare(10L, "theory");
      assertThatThrownBy(() -> controller.get(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.candidates(9L, new CompetitionFormalCandidateExamQueryReq()))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.preflight(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.candidate(8L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.start(9L, new CompetitionFormalExamSessionStartReq(true)))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void forceSubmitCannotReadWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var service = context.getBean(CompetitionFormalExamSessionApplicationService.class);
      authenticate("competition:exam-session:force-submit");
      controller.forceSubmit(9L, 8L, new CompetitionFormalExamForceSubmitReq("现场设备异常"));
      verify(service).forceSubmit(9L, 8L, "现场设备异常");
      assertThatThrownBy(() -> controller.get(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.byStage(10L, "theory"))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.forceOffline(9L, 8L))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).forceOffline(any(), any());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void forceOfflineCannotReadWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var service = context.getBean(CompetitionFormalExamSessionApplicationService.class);
      authenticate("competition:exam-session:force-offline");
      controller.forceOffline(9L, 8L);
      verify(service).forceOffline(9L, 8L);
      assertThatThrownBy(() -> controller.get(9L)).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.forceSubmit(
          9L, 8L, new CompetitionFormalExamForceSubmitReq("现场设备异常")))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void twoArgConstructorStillWorks() {
    new CompetitionFormalExamSessionController(
        mock(CompetitionFormalExamSessionApplicationService.class),
        mock(CompetitionFormalExamScoringService.class));
  }

  @Test
  void viewCanIssueRealtimeTicket() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var consoleRealtime = context.getBean(FormalExamConsoleRealtimeApplicationService.class);
      authenticate("competition:exam-session:view");
      controller.realtimeTicket(9L);
      verify(consoleRealtime).issueTicket(9L);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void startCannotIssueRealtimeTicketWithoutView() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamSessionController.class);
      var consoleRealtime = context.getBean(FormalExamConsoleRealtimeApplicationService.class);
      authenticate("competition:exam-session:start");
      assertThatThrownBy(() -> controller.realtimeTicket(9L))
          .isInstanceOf(AccessDeniedException.class);
      verify(consoleRealtime, never()).issueTicket(any());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void authenticate(String authority) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "test", "", List.of(new SimpleGrantedAuthority(authority))));
  }
}
