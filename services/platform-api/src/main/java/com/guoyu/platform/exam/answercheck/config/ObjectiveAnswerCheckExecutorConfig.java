package com.guoyu.platform.exam.answercheck.config;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 本功能专用线程池（方案 §11.8）：全部 AbortPolicy，禁止 CallerRunsPolicy。
 *
 * <p>编排池 core=1/max=2/queue=10；Worker 池 core=max=concurrency/queue 与并发同源。
 */
@Configuration
public class ObjectiveAnswerCheckExecutorConfig {

  @Bean(name = "objectiveAnswerCheckExecutor")
  public ThreadPoolTaskExecutor objectiveAnswerCheckExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("objective-answer-check-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(name = "objectiveAnswerCheckWorkerExecutor")
  public ThreadPoolTaskExecutor objectiveAnswerCheckWorkerExecutor(
      ObjectiveAnswerCheckProperties properties) {
    int concurrency = Math.max(1, properties.getConcurrency());
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(concurrency);
    executor.setMaxPoolSize(concurrency);
    executor.setQueueCapacity(concurrency);
    executor.setThreadNamePrefix("objective-answer-check-worker-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /** 租约心跳调度（每 15 秒续租 60 秒）；不使用裸后端无限循环。 */
  @Bean(name = "objectiveAnswerCheckLeaseScheduler", destroyMethod = "shutdownNow")
  public java.util.concurrent.ScheduledExecutorService objectiveAnswerCheckLeaseScheduler() {
    java.util.concurrent.ScheduledThreadPoolExecutor scheduler =
        new java.util.concurrent.ScheduledThreadPoolExecutor(1, runnable -> {
          Thread thread = new Thread(runnable, "objective-answer-check-lease");
          thread.setDaemon(true);
          return thread;
        });
    scheduler.setKeepAliveTime(60, TimeUnit.SECONDS);
    scheduler.allowCoreThreadTimeOut(true);
    return scheduler;
  }
}
