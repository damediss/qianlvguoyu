package com.guoyu.platform.exam.reviewsync.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService.SubjectiveGradeResult;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 老接口 /appreviewai/grade 的闸口接线（拦截方案 §4.4）：
 * 垃圾答案正常通过 controller 校验后由服务层闸口拦截，响应携带 gradeSource=rule。
 */
class AppReviewAiControllerGuardTest {

  private AiGradeApplicationService gradeService;
  private AppReviewAiController controller;

  @BeforeEach
  void setUp() {
    gradeService = mock(AiGradeApplicationService.class);
    controller = new AppReviewAiController(gradeService);
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        42L, 10159L, "student", "学员", "app", List.of(), List.of(), "");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    TenantContextHolder.setTenantId(10159L);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private Map<String, Object> junkBody() {
    Map<String, Object> body = new HashMap<>();
    body.put("questionId", 902L);
    body.put("stem", "题干：论述绿色制造");
    body.put("referenceAnswer", "低碳、零排放、循环经济");
    body.put("userAnswer", "2");
    body.put("questionType", "essay");
    body.put("score", 1);
    return body;
  }

  @Test
  void junkAnswerReturnsRuleSourceFromService() {
    when(gradeService.grade(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(),
        anyString(), anyInt())).thenReturn(new SubjectiveGradeResult(true, 0, false,
        "系统未识别到有效作答，本题记 0 分。想看答案可以点下方「查看解析」；愿意动手可以重新作答。",
        "rule"));

    LegacyAppResponse response = controller.grade(junkBody());

    assertEquals(0, response.code());
    Map<?, ?> data = assertInstanceOf(Map.class, response.data());
    assertEquals(0, data.get("score"));
    assertEquals("rule", data.get("gradeSource"));
    verify(gradeService).grade(anyLong(), anyLong(), anyLong(), anyString(), anyString(),
        anyString(), anyString(), anyInt());
  }

  @Test
  void blankAnswerRejectedBeforeService() {
    Map<String, Object> body = junkBody();
    body.put("userAnswer", "   ");
    assertThrows(BusinessException.class, () -> controller.grade(body));
  }

  @Test
  void objectiveTypeRejectedBeforeService() {
    Map<String, Object> body = junkBody();
    body.put("questionType", "single");
    assertThrows(BusinessException.class, () -> controller.grade(body));
  }
}
