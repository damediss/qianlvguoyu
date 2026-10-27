package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** 解析生成每学员每日限流（§5.9：默认 20、超限拒绝、Redis 异常 fail-closed）。 */
class QuestionResolveRateLimitServiceTest {

  private final Map<String, String> dictValues = new HashMap<>();
  private StringRedisTemplate redisTemplate;
  private QuestionResolveRateLimitService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, DictEntity.class);
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    dictValues.clear();
    redisTemplate = mock(StringRedisTemplate.class);
    DictMapper dictMapper = mock(DictMapper.class);
    service = new QuestionResolveRateLimitService(redisTemplate, dictMapper);
    when(dictMapper.selectList(any())).thenAnswer(inv -> {
      if (inv.getArgument(0) instanceof AbstractWrapper<?, ?, ?> wrapper) {
        wrapper.getSqlSegment();
        String code = wrapper.getParamNameValuePairs().values().stream()
            .filter(String.class::isInstance).map(String.class::cast)
            .filter(s -> s.contains("_")).findFirst().orElse(null);
        if (code != null && dictValues.containsKey(code)) {
          DictEntity dict = new DictEntity();
          dict.setValue(dictValues.get(code));
          return java.util.List.of(dict);
        }
      }
      return java.util.List.of();
    });
  }

  @AfterEach
  void tearDown() {
    dictValues.clear();
  }

  @Test
  void defaultLimitTwentyAllowsAcquire() {
    stubScriptResult(1L);
    assertDoesNotThrow(() -> {
      for (int i = 0; i < 20; i++) {
        service.acquire(10159L, 700L);
      }
    });
  }

  @Test
  void overLimitRejected() {
    stubScriptResult(-1L);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.acquire(10159L, 700L));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
  }

  @Test
  void redisExceptionFailsClosed() {
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
        .thenThrow(new RuntimeException("redis down"));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.acquire(10159L, 700L));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
  }

  @Test
  void dictOverridesLimitAndPassesToScript() {
    dictValues.put("question_resolve_daily_limit", "3");
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
        .thenAnswer(inv -> {
          // execute(script, keys, arg1=limit, arg2=ttl)：varargs 作为独立参数传入
          assertEquals("3", String.valueOf(inv.getArguments()[2]));
          return 1L;
        });
    assertDoesNotThrow(() -> service.acquire(10159L, 700L));
  }

  @SuppressWarnings("unchecked")
  private void stubScriptResult(long value) {
    when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
        .thenReturn(value);
  }
}
