package com.guoyu.platform.exam.answercheck.config;

import com.guoyu.platform.ai.config.ObjectiveAnswerJevProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册本功能业务配置与 AI 侧 JEV 配置，启动即校验范围。 */
@Configuration
@EnableConfigurationProperties({ObjectiveAnswerCheckProperties.class,
    ObjectiveAnswerJevProperties.class})
public class ObjectiveAnswerCheckConfig {
}
