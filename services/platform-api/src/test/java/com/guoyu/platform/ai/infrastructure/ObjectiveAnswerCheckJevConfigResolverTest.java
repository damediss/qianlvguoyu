package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.exam.answercheck.config.ObjectiveAnswerCheckProperties;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** v1.5 配置解析：DB 只提供非敏感连接配置，真实密钥只认环境变量，占位值永不发送。 */
@ExtendWith(MockitoExtension.class)
class ObjectiveAnswerCheckJevConfigResolverTest {

  @Mock
  private ExamFaqModelConfigMapper mapper;

  private ObjectiveAnswerCheckProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ObjectiveAnswerCheckProperties();
    properties.setConcurrency(20);
  }

  private ExamFaqModelConfigEntity config(String status) {
    ExamFaqModelConfigEntity cfg = new ExamFaqModelConfigEntity();
    cfg.setTenantId(10159L);
    cfg.setModelCode(ObjectiveAnswerCheckJevConfigResolver.MODEL_CODE);
    cfg.setModelId("ateve-jev-v1");
    cfg.setBaseUrl("https://tokendance.space/gateway");
    cfg.setStatus(status);
    cfg.setJevConcurrency(5);
    cfg.setJevReadTimeoutSeconds(30);
    return cfg;
  }

  private ObjectiveAnswerCheckJevConfigResolver resolver(String envKey) {
    return new ObjectiveAnswerCheckJevConfigResolver(mapper, properties, envKey);
  }

  @Test
  void enabledConfigWithEnvKeyIsReadyAndExposesNonSensitiveSnapshot() {
    when(mapper.selectOne(any())).thenReturn(config("enabled"));
    ObjectiveAnswerCheckJevConfigResolver resolver = resolver("real-secret");

    ObjectiveAnswerCheckAiDescriptor descriptor = resolver.descriptorFor(10159L);

    assertTrue(descriptor.credentialsReady());
    assertEquals("OBJECTIVE_ANSWER_CHECK_JEV_MODEL", descriptor.modelCode());
    assertEquals("ateve-jev-v1", descriptor.modelId());
    assertEquals("https://tokendance.space/gateway", descriptor.baseUrl());
    assertEquals(5, descriptor.jevConcurrency());
    assertEquals(30, descriptor.jevReadTimeoutSeconds());
  }

  @Test
  void databasePlaceholderIsNeverUsableCredential() {
    when(mapper.selectOne(any())).thenReturn(config("enabled"));
    ObjectiveAnswerCheckAiDescriptor descriptor = resolver("模型API Key走配置文件")
        .descriptorFor(10159L);
    assertFalse(descriptor.credentialsReady());
  }

  @Test
  void disabledOrMissingConfigIsNotReady() {
    when(mapper.selectOne(any())).thenReturn(config("disabled"));
    assertFalse(resolver("real-secret").descriptorFor(10159L).credentialsReady());

    when(mapper.selectOne(any())).thenReturn(null);
    ObjectiveAnswerCheckAiDescriptor missing = resolver("real-secret").descriptorFor(10159L);
    assertFalse(missing.credentialsReady());
    assertEquals(5, missing.jevConcurrency());
    assertEquals(30, missing.jevReadTimeoutSeconds());
  }

  @Test
  void invalidDbReadTimeoutMakesConfigUnavailable() {
    ExamFaqModelConfigEntity cfg = config("enabled");
    cfg.setJevReadTimeoutSeconds(3);
    when(mapper.selectOne(any())).thenReturn(cfg);
    assertFalse(resolver("real-secret").descriptorFor(10159L).credentialsReady());
  }

  @Test
  void dbConcurrencyIsCappedByWorkerPoolUpperBound() {
    ExamFaqModelConfigEntity cfg = config("enabled");
    cfg.setJevConcurrency(50);
    when(mapper.selectOne(any())).thenReturn(cfg);
    assertEquals(20, resolver("real-secret").descriptorFor(10159L).jevConcurrency());
  }

  @Test
  void runtimeUsesBatchSnapshotAndEnvKeyInsteadOfCurrentDbConfig() {
    ObjectiveAnswerCheckJevConfigResolver resolver = resolver("real-secret");
    ObjectiveAnswerCheckCallContext context = new ObjectiveAnswerCheckCallContext(10159L, 7L, 11L,
        22L, 33L, 1, "snapshot-model", "https://snapshot.local/gateway", 45);

    ObjectiveAnswerCheckJevConfigResolver.Runtime runtime = resolver.runtimeFor(context);

    assertEquals("snapshot-model", runtime.modelId());
    assertEquals("https://snapshot.local/gateway", runtime.baseUrl());
    assertEquals(45, runtime.readTimeoutSeconds());
    assertEquals("real-secret", runtime.apiKey());
  }

  @Test
  void runtimeRejectsMissingEnvKeyEvenWhenBatchSnapshotIsComplete() {
    ObjectiveAnswerCheckJevConfigResolver resolver = resolver("");
    ObjectiveAnswerCheckCallContext context = new ObjectiveAnswerCheckCallContext(10159L, 7L, 11L,
        22L, 33L, 1, "snapshot-model", "https://snapshot.local/gateway", 45);
    assertThrows(IllegalStateException.class, () -> resolver.runtimeFor(context));
  }
}
