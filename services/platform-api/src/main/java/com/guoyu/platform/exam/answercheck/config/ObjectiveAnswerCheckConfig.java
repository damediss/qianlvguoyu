package com.guoyu.platform.exam.answercheck.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册本功能业务配置，启动即校验范围；JEV 连接配置走 exam_faq_model_config。 */
@Configuration
@EnableConfigurationProperties(ObjectiveAnswerCheckProperties.class)
public class ObjectiveAnswerCheckConfig {
}
