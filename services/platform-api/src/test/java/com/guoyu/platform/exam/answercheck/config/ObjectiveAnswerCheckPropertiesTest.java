package com.guoyu.platform.exam.answercheck.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 配置范围启动校验（方案 §9.1）：非法配置必须拒绝启动，而不是运行时静默降级。 */
class ObjectiveAnswerCheckPropertiesTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private Set<?> violations(Object bean) {
    return validator.validate(bean);
  }

  @Test
  void defaultConfigurationIsValidAndCapsAtTwoThousand() {
    ObjectiveAnswerCheckProperties properties = new ObjectiveAnswerCheckProperties();
    assertTrue(violations(properties).isEmpty());
    assertEquals(2000, properties.effectiveMaxBatchSize());

    properties.setBatchMaxSize(200);
    assertEquals(200, properties.effectiveMaxBatchSize());
  }

  @Test
  void illegalRangesRejectStartup() {
    ObjectiveAnswerCheckProperties properties = new ObjectiveAnswerCheckProperties();
    properties.setBatchMaxSize(2001);
    assertTrue(!violations(properties).isEmpty());

    ObjectiveAnswerCheckProperties concurrency = new ObjectiveAnswerCheckProperties();
    concurrency.setConcurrency(21);
    assertTrue(!violations(concurrency).isEmpty());

    ObjectiveAnswerCheckProperties threshold = new ObjectiveAnswerCheckProperties();
    threshold.setConfidenceThreshold(1.2);
    assertTrue(!violations(threshold).isEmpty());

    ObjectiveAnswerCheckProperties margin = new ObjectiveAnswerCheckProperties();
    margin.setMarginThreshold(-0.1);
    assertTrue(!violations(margin).isEmpty());
  }
}
