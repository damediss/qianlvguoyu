package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.dto.DictCreateReq;
import com.guoyu.platform.system.mapper.DictItemMapper;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DictApplicationServiceTest {

  private final DictMapper dictMapper = Mockito.mock(DictMapper.class);
  private final DictItemMapper dictItemMapper = Mockito.mock(DictItemMapper.class);
  private final DictApplicationService service = new DictApplicationService(dictMapper, dictItemMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("dict:create"),"asd@qq.com"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRejectsDuplicateDictCode() {
    when(dictMapper.exists(any(Wrapper.class))).thenReturn(true);

    DictCreateReq req = new DictCreateReq("work_type", "工种", "enabled","1");

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Dict code already exists");
  }
}
