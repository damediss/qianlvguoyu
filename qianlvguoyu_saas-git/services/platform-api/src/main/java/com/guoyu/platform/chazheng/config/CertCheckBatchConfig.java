package com.guoyu.platform.chazheng.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 查证批次配置 —— 线程池 + 限流参数。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chazheng.batch")
public class CertCheckBatchConfig {

  /** 核心线程数(常驻) */
  private int corePoolSize = 3;

  /** 最大线程数(高峰扩容) */
  private int maxPoolSize = 5;

  /** 队列容量(最大排队用户数) */
  private int queueCapacity = 1000;

  /** 单用户最大重试次数 */
  private int maxRetry = 2;

  /** 每秒最大请求数(QPS 限流) */
  private double rateLimitPerSecond = 10.0;

  public ThreadPoolTaskExecutor createExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("cert-check-");
    // 队列满时由调用线程执行(降级同步,不丢任务)
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // 关闭时等待任务完成
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }
}
