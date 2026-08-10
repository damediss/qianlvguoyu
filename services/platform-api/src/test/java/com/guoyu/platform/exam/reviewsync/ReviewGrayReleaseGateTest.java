package com.guoyu.platform.exam.reviewsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanDayMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.exam.reviewsync.service.PhpResultWritebackService;
import com.guoyu.platform.integration.php.PhpExamQueryService;
import com.guoyu.platform.exam.reviewsync.service.ReviewLlmAssistService;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** D8 灰度拦截（方案 §15.2）：开关 + exam/user 双粒度白名单 */
class ReviewGrayReleaseGateTest {

  private final DictMapper dictMapper = mock(DictMapper.class);
  private final AiReviewGrayWhitelistMapper whitelistMapper = mock(AiReviewGrayWhitelistMapper.class);
  private final AiReviewPlanMapper planMapper = mock(AiReviewPlanMapper.class);
  private final ReviewPlanApplicationService service = new ReviewPlanApplicationService(
      planMapper,
      mock(AiReviewPlanDayMapper.class),
      mock(AiReviewAnswerMapper.class),
      mock(AiReviewWrongItemMapper.class),
      mock(AiReviewMockPaperMapper.class),
      mock(AiReviewMockPaperItemMapper.class),
      whitelistMapper,
      mock(PhpExamQueryService.class),
      mock(PhpResultWritebackService.class),
      mock(ReviewLlmAssistService.class),
      dictMapper,
      new ObjectMapper());

  private void stubGraySwitch(String value) {
    when(dictMapper.selectList(any())).thenAnswer(invocation -> {
      if (value == null) {
        return List.of();
      }
      DictEntity dict = new DictEntity();
      dict.setValue(value);
      return List.of(dict);
    });
  }

  @Test
  void grayDisabledPassesThroughWithoutWhitelistQuery() {
    stubGraySwitch(null); // 字典缺失 = 全员开放
    // 拦截放行后走到 requirePlan → 计划不存在（证明未被灰度拦截）
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.progress(608L, 451L, 1L));
    assertEquals("复习计划不存在，请先进入同步练习", ex.getMessage());
    verify(whitelistMapper, never()).selectCount(any());
  }

  @Test
  void grayEnabledBlocksNonWhitelistedUser() {
    stubGraySwitch("true");
    when(whitelistMapper.selectCount(any())).thenReturn(0L);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.progress(608L, 451L, 1L));
    assertEquals("同步练习正在小范围试点中，敬请期待", ex.getMessage());
    // 未进入业务逻辑
    verify(planMapper, never()).selectOne(any());
  }

  @Test
  void grayEnabledAllowsExamWhitelistHit() {
    stubGraySwitch("true");
    // exam 命中（第一次 selectCount>0），不再查 user
    when(whitelistMapper.selectCount(any())).thenReturn(1L);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.progress(608L, 451L, 1L));
    assertEquals("复习计划不存在，请先进入同步练习", ex.getMessage());
  }

  @Test
  void grayEnabledAllowsUserWhitelistHit() {
    stubGraySwitch("true");
    // exam 未命中、user 命中
    when(whitelistMapper.selectCount(any())).thenReturn(0L, 1L);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.progress(609L, 999L, 1L));
    assertEquals("复习计划不存在，请先进入同步练习", ex.getMessage());
  }
}
