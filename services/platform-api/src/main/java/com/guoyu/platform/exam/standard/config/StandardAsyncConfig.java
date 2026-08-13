package com.guoyu.platform.exam.standard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 细目表 LLM 概括线程池（AbortPolicy，坑#12：禁止 CallerRunsPolicy 退化为 HTTP 线程同步执行）。
 */
@Configuration
public class StandardAsyncConfig {

  @Bean(name = "standardSummarizeExecutor")
  public ThreadPoolTaskExecutor standardSummarizeExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("standard-summarize-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }
}
