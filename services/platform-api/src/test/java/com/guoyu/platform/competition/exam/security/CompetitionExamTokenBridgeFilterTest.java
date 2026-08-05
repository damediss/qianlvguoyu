package com.guoyu.platform.competition.exam.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
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

  private MockHttpServletRequest request(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET", "/api/competition/exam/waiting");
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
