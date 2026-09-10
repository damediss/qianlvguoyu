package com.guoyu.platform.competition.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerLoginReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerRealtimeAckReq;
import com.guoyu.platform.competition.exam.realtime.FormalExamPlayerRealtimeApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamPlayerApplicationService;
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

class CompetitionFormalExamPlayerPermissionTest {

  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean
    CompetitionFormalExamPlayerApplicationService service() {
      return mock(CompetitionFormalExamPlayerApplicationService.class);
    }

    @Bean
    FormalExamPlayerRealtimeApplicationService realtime() {
      return mock(FormalExamPlayerRealtimeApplicationService.class);
    }

    @Bean
    CompetitionFormalExamPlayerController controller(
        CompetitionFormalExamPlayerApplicationService service,
        FormalExamPlayerRealtimeApplicationService realtime) {
      return new CompetitionFormalExamPlayerController(service, realtime);
    }
  }

  @Test
  void oneArgConstructorStillWorks() {
    new CompetitionFormalExamPlayerController(mock(CompetitionFormalExamPlayerApplicationService.class));
  }

  @Test
  void loginDoesNotRequireExamRole() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamPlayerController.class);
      var service = context.getBean(CompetitionFormalExamPlayerApplicationService.class);
      SecurityContextHolder.clearContext();
      controller.login(new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12"));
      verify(service).login(new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void contextAndEnterRequireExamRole() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamPlayerController.class);
      var service = context.getBean(CompetitionFormalExamPlayerApplicationService.class);
      var realtime = context.getBean(FormalExamPlayerRealtimeApplicationService.class);
      authenticate("ROLE_USER");
      assertThatThrownBy(() -> controller.context("9001")).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.enter("9001")).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.paper("9001")).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.saveAnswer("9001", "9301", null))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.syncAnswers("9001", null))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.submit("9001", null))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.status("9001"))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.realtimeTicket())
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> controller.realtimeAck(new CompetitionFormalExamPlayerRealtimeAckReq("1")))
          .isInstanceOf(AccessDeniedException.class);
      verify(service, never()).context("9001");
      verify(service, never()).status("9001");
      verify(service, never()).enter("9001");
      verify(service, never()).paper("9001");
      verify(service, never()).submit("9001", null);
      verify(realtime, never()).issueTicket();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void examRoleCanReadContextAndEnter() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(CompetitionFormalExamPlayerController.class);
      var service = context.getBean(CompetitionFormalExamPlayerApplicationService.class);
      var realtime = context.getBean(FormalExamPlayerRealtimeApplicationService.class);
      authenticate("ROLE_COMPETITION_EXAM");
      controller.context("9001");
      controller.status("9001");
      controller.enter("9001");
      controller.paper("9001");
      controller.saveAnswer("9001", "9301", null);
      controller.syncAnswers("9001", null);
      controller.submit("9001", null);
      controller.realtimeTicket();
      controller.realtimeAck(new CompetitionFormalExamPlayerRealtimeAckReq("88"));
      verify(service).context("9001");
      verify(service).status("9001");
      verify(service).enter("9001");
      verify(service).paper("9001");
      verify(service).saveAnswer("9001", "9301", null);
      verify(service).syncAnswers("9001", null);
      verify(service).submit("9001", null);
      verify(realtime).issueTicket();
      verify(realtime).ackFromPlayer("88");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void authenticate(String authority) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "player", "", List.of(new SimpleGrantedAuthority(authority))));
  }
}
