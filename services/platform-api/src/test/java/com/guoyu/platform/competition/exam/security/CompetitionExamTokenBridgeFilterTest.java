package com.guoyu.platform.competition.exam.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.service.CompetitionFormalExamAccessSessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionExamTokenBridgeFilterTest {
  private final StpLogic examStpLogic = mock(StpLogic.class);
  private final CompetitionPlayerIdentityMapper playerMapper = mock(CompetitionPlayerIdentityMapper.class);
  private final CompetitionExamTokenBridgeFilter filter =
      new CompetitionExamTokenBridgeFilter(examStpLogic, playerMapper);

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  @Test
  void nonExamTokenCannotFallThroughToAnotherAuthenticationNamespace() throws Exception {
    MockHttpServletRequest request = request("platform-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(examStpLogic.getLoginIdByToken("platform-token")).thenReturn(null);
    TenantContextHolder.setTenantId(1L);

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verifyNoInteractions(chain, playerMapper);
  }

  @Test
  void dedicatedExamTokenCreatesOnlyExamPrincipal() throws Exception {
    MockHttpServletRequest request = request("exam-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(examStpLogic.getLoginIdByToken("exam-token")).thenReturn(9L);
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(9L);
    player.setTenantId(1L);
    player.setStatus("active");
    when(playerMapper.selectById(9L)).thenReturn(player);
    TenantContextHolder.setTenantId(1L);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isInstanceOf(CompetitionExamPrincipal.class);
  }

  @Test
  void pendingActivationExamTokenStillBridges() throws Exception {
    MockHttpServletRequest request = request("exam-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(examStpLogic.getLoginIdByToken("exam-token")).thenReturn(9L);
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(9L);
    player.setTenantId(1L);
    player.setStatus("pending_activation");
    when(playerMapper.selectById(9L)).thenReturn(player);
    TenantContextHolder.setTenantId(1L);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isInstanceOf(CompetitionExamPrincipal.class);
  }

  @Test
  void formalExamPlayerPathRejectsTokenWithoutAccessSession() throws Exception {
    CompetitionFormalExamAccessSessionService accessSessions =
        mock(CompetitionFormalExamAccessSessionService.class);
    CompetitionExamTokenBridgeFilter formalFilter =
        new CompetitionExamTokenBridgeFilter(examStpLogic, playerMapper, accessSessions);
    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET", "/api/competition/formal-exam/player/sessions/9/context");
    request.addHeader("Authorization", "Bearer exam-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(examStpLogic.getLoginIdByToken("exam-token")).thenReturn(9L);
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(9L);
    player.setTenantId(1L);
    player.setStatus("active");
    when(playerMapper.selectById(9L)).thenReturn(player);
    when(accessSessions.requireActiveByToken("exam-token", 1L, 9L)).thenReturn(null);
    TenantContextHolder.setTenantId(1L);

    formalFilter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verifyNoInteractions(chain);
  }

  @Test
  void formalExamPlayerPathBindsActiveAccessSessionToPrincipal() throws Exception {
    CompetitionFormalExamAccessSessionService accessSessions =
        mock(CompetitionFormalExamAccessSessionService.class);
    CompetitionExamTokenBridgeFilter formalFilter =
        new CompetitionExamTokenBridgeFilter(examStpLogic, playerMapper, accessSessions);
    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET", "/api/competition/formal-exam/player/sessions/9/context");
    request.addHeader("Authorization", "Bearer exam-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(examStpLogic.getLoginIdByToken("exam-token")).thenReturn(9L);
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(9L);
    player.setTenantId(1L);
    player.setStatus("active");
    when(playerMapper.selectById(9L)).thenReturn(player);
    CompetitionFormalExamAccessSessionEntity access = new CompetitionFormalExamAccessSessionEntity();
    access.setId(55L);
    access.setTenantId(1L);
    access.setPlayerId(9L);
    access.setExamSessionId(9001L);
    access.setCandidateExamId(9101L);
    access.setGeneration(2);
    access.setStatus("active");
    when(accessSessions.requireActiveByToken("exam-token", 1L, 9L)).thenReturn(access);
    TenantContextHolder.setTenantId(1L);

    formalFilter.doFilter(request, response, chain);

    CompetitionExamPrincipal principal = (CompetitionExamPrincipal)
        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal.getAccessSessionId()).isEqualTo(55L);
    assertThat(principal.getGeneration()).isEqualTo(2);
    assertThat(principal.getExamSessionId()).isEqualTo(9001L);
    assertThat(principal.getCandidateExamId()).isEqualTo(9101L);
    verify(chain).doFilter(request, response);
  }

  @Test
  void formalExamLoginSkipsTokenBridge() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(
        "POST", "/api/competition/formal-exam/player/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(playerMapper);
  }

  private MockHttpServletRequest request(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET", "/api/competition/exam/waiting");
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
