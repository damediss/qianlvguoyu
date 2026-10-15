package com.guoyu.platform.exam.reviewsync;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.reviewsync.service.ReviewLlmAssistService;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.system.mapper.DictMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 连对/连错鼓励防打扰（D9，方案 §15.3）：每档每日一次 + 每 5 题最多 1 条 */
class ReviewLlmAssistStreakTest {

  private ReviewLlmAssistService service;

  @BeforeEach
  void setUp() {
    service = new ReviewLlmAssistService(
        Mockito.mock(FaqLlmClient.class),
        Mockito.mock(FaqModelConfigApplicationService.class),
        Mockito.mock(DictMapper.class),
        Mockito.mock(AiGenerationDraftMapper.class),
        Mockito.mock(ThreadPoolTaskExecutor.class));
  }

  @Test
  void wrongTierFourFiresOnceWithFallbackCopy() {
    // W1.5：连错 4 档兜底文案（方案 §4.5 逐字），触发一次后当日不再发
    String copy = service.streakEncouragement(1L, 1, "wrong", 4, 10);
    assertNotNull(copy);
    assertTrue(copy.contains("先停一停"), "应命中 §4.5 兜底句，实际=" + copy);
    assertNull(service.streakEncouragement(1L, 1, "wrong", 4, 11));
  }

  @Test
  void tierZeroNeverFires() {
    assertNull(service.streakEncouragement(1L, 1, "correct", 0, 10));
  }

  @Test
  void eachTierFiresOncePerDay() {
    // 首次命中档 3 → 发文案；同档再次 → null（每档每日一次）
    assertNotNull(service.streakEncouragement(1L, 1, "correct", 3, 3));
    assertNull(service.streakEncouragement(1L, 1, "correct", 3, 10));
    // 不同档位不受影响；不同天（dayIndex）互不影响
    assertNotNull(service.streakEncouragement(1L, 1, "correct", 5, 15));
    assertNotNull(service.streakEncouragement(1L, 2, "correct", 3, 3));
    // 不同计划互不影响
    assertNotNull(service.streakEncouragement(2L, 1, "correct", 3, 3));
  }

  @Test
  void spacingSuppressesWithinFiveAnswers() {
    // answerCount=3 时发放档 3；answerCount=5 时距上一条不足 5 题 → 压制
    assertNotNull(service.streakEncouragement(3L, 1, "correct", 3, 3));
    assertNull(service.streakEncouragement(3L, 1, "correct", 5, 5));
    // 间隔满 5 题后下一档可发
    assertNotNull(service.streakEncouragement(3L, 1, "correct", 5, 8));
  }

  @Test
  void wrongStreakUsesWrongPool() {
    assertNotNull(service.streakEncouragement(4L, 1, "wrong", 2, 5));
    assertNull(service.streakEncouragement(4L, 1, "wrong", 2, 12));
    assertNotNull(service.streakEncouragement(4L, 1, "wrong", 3, 12));
  }

  @Test
  void unknownTierReturnsNull() {
    assertNull(service.streakEncouragement(5L, 1, "correct", 7, 30));
  }
}
