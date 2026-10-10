package com.guoyu.platform.exam.reviewsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** AI 判分限流：Lua 原子计次双维度（每用户每分钟/每日），桩 StringRedisTemplate（方案 §8.1） */
class AiGradeRateLimitServiceTest {

  private StringRedisTemplate redisTemplate;
  private DictMapper dictMapper;
  private AiGradeRateLimitService service;

  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    dictMapper = mock(DictMapper.class);
    service = new AiGradeRateLimitService(redisTemplate, dictMapper);
    when(dictMapper.selectList(any())).thenReturn(List.of());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void acquireAllowedWhenUnderLimit() {
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(),
        anyString(), anyString(), anyString(), anyString())).thenReturn(1L);
    service.acquire(10159L, 7L);

    ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(DefaultRedisScript.class), keysCaptor.capture(),
        // 默认阈值：每分钟 10、每日 200
        eq("10"), eq("200"), anyString(), anyString());
    List<String> keys = keysCaptor.getValue();
    assertEquals(2, keys.size());
    assertTrue(keys.get(0).startsWith("grade:rate:min:10159:7:"));
    assertTrue(keys.get(1).startsWith("grade:rate:day:10159:7:"));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void limitsComeFromDict() {
    DictEntity dict = new DictEntity();
    dict.setValue("3");
    when(dictMapper.selectList(any())).thenReturn(List.of(dict));
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(),
        anyString(), anyString(), anyString(), anyString())).thenReturn(1L);
    service.acquire(10159L, 7L);
    verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(),
        eq("3"), eq("3"), anyString(), anyString());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void exceededThrowsTooManyRequests() {
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(),
        anyString(), anyString(), anyString(), anyString())).thenReturn(-1L);
    BusinessException ex = assertThrows(BusinessException.class, () -> service.acquire(10159L, 7L));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    assertEquals("AI 判分次数已达上限，请自评", ex.getMessage());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void redisFailureFailsClosed() {
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(),
        anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new RuntimeException("redis down"));
    BusinessException ex = assertThrows(BusinessException.class, () -> service.acquire(10159L, 7L));
    assertEquals(ErrorCode.SERVICE_UNAVAILABLE, ex.getCode());
  }
}
