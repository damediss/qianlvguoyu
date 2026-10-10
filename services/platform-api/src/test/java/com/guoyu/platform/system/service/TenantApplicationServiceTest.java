package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.system.dto.TenantCreateReq;
import com.guoyu.platform.system.mapper.TenantMapper;
import org.junit.jupiter.api.Test;

class TenantApplicationServiceTest {

  private final TenantMapper tenantMapper = org.mockito.Mockito.mock(TenantMapper.class);
  private final TenantApplicationService service = new TenantApplicationService(tenantMapper);

  @Test
  void createRejectsDuplicateTenantCode() {
    when(tenantMapper.exists(any(Wrapper.class))).thenReturn(true);

    TenantCreateReq req = new TenantCreateReq(
        "demo",
        "演示租户",
        "enabled",
        null,
        null,
        null
    );

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Tenant code already exists");
  }
}
