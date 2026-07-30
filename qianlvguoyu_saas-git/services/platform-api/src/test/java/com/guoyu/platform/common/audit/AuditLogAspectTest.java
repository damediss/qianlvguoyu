package com.guoyu.platform.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.AuditLogMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditLogAspectTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TraceIdHolder.clear();
  }

  @Test
  void currentPrincipalCanBeAbsentWithoutFailure() {
    AuditLogMapper mapper = Mockito.mock(AuditLogMapper.class);
    AuditLogAspect aspect = new AuditLogAspect(mapper, new com.fasterxml.jackson.databind.ObjectMapper());

    aspect.writeAuditLog(Mockito.mock(org.aspectj.lang.JoinPoint.class), auditLog(), null);

    Mockito.verifyNoInteractions(mapper);
  }

  @Test
  void writesAuditForAuthenticatedUser() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("tenant:create"),"asd@qq.com"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
    TraceIdHolder.setTraceId("req-test");

    AuditLogMapper mapper = Mockito.mock(AuditLogMapper.class);
    AuditLogAspect aspect = new AuditLogAspect(mapper, new com.fasterxml.jackson.databind.ObjectMapper());
    org.aspectj.lang.JoinPoint joinPoint = Mockito.mock(org.aspectj.lang.JoinPoint.class);
    org.aspectj.lang.Signature signature = Mockito.mock(org.aspectj.lang.Signature.class);
    Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] {123L});
    Mockito.when(joinPoint.getSignature()).thenReturn(signature);
    Mockito.when(signature.toShortString()).thenReturn("TenantController.update(..)");

    aspect.writeAuditLog(joinPoint, auditLog(), null);

    ArgumentCaptor<com.guoyu.platform.system.entity.AuditLogEntity> captor =
        ArgumentCaptor.forClass(com.guoyu.platform.system.entity.AuditLogEntity.class);
    Mockito.verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
    assertThat(captor.getValue().getUserId()).isEqualTo(1000L);
    assertThat(captor.getValue().getTargetId()).isEqualTo(123L);
    assertThat(captor.getValue().getRequestId()).isEqualTo("req-test");
  }

  @Test
  void omitsBinaryPayloadFromAuditDetailJson() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("file:upload"),"asd@qq.com"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );

    AuditLogMapper mapper = Mockito.mock(AuditLogMapper.class);
    AuditLogAspect aspect = new AuditLogAspect(mapper, new com.fasterxml.jackson.databind.ObjectMapper());
    org.aspectj.lang.JoinPoint joinPoint = Mockito.mock(org.aspectj.lang.JoinPoint.class);
    org.aspectj.lang.Signature signature = Mockito.mock(org.aspectj.lang.Signature.class);
    byte[] payload = new byte[8192];
    Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] {payload});
    Mockito.when(joinPoint.getSignature()).thenReturn(signature);
    Mockito.when(signature.toShortString()).thenReturn("FileController.uploadBinary(..)");

    aspect.writeAuditLog(joinPoint, fileAuditLog(), null);

    ArgumentCaptor<com.guoyu.platform.system.entity.AuditLogEntity> captor =
        ArgumentCaptor.forClass(com.guoyu.platform.system.entity.AuditLogEntity.class);
    Mockito.verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getDetailJson()).contains("\"type\":\"byte[]\"");
    assertThat(captor.getValue().getDetailJson()).contains("\"length\":8192");
    assertThat(captor.getValue().getDetailJson().length()).isLessThan(1024);
  }

  private AuditLog fileAuditLog() {
    return new AuditLog() {
      @Override
      public String module() {
        return "file";
      }

      @Override
      public String action() {
        return "file:upload";
      }

      @Override
      public String targetType() {
        return "file";
      }

      @Override
      public boolean includeResult() {
        return false;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return AuditLog.class;
      }
    };
  }

  private AuditLog auditLog() {
    return new AuditLog() {
      @Override
      public String module() {
        return "system";
      }

      @Override
      public String action() {
        return "tenant:update";
      }

      @Override
      public String targetType() {
        return "tenant";
      }

      @Override
      public boolean includeResult() {
        return false;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return AuditLog.class;
      }
    };
  }
}
