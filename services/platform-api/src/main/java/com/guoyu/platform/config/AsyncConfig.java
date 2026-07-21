package com.guoyu.platform.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Flowable 工作流引擎异步任务执行器（job executor）。
   * Flowable Spring Boot Starter 通过 @Qualifier("applicationTaskExecutor") 注入。
   */
  @Bean(name = "applicationTaskExecutor")
  public AsyncTaskExecutor applicationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("flowable-job-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(name = "signUpExportExecutor")
  public ThreadPoolTaskExecutor signUpExportExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("sign-up-export-");
    executor.initialize();
    return executor;
  }

  /**
   * 批次编排池（创建任务、触发异步、checkBatchCompletion）。
   * 生产只跑 1 个批次，allOf().join() 阻塞 1 线程 ~20min，core=2 留 1 线程给 resume/retry 等操作。
   */
  @Bean(name = "faqConversionExecutor")
  public ThreadPoolTaskExecutor faqConversionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("faq-conversion-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 批次内记录级并行池（每条记录一个 Worker 线程，Semaphore 限流）。
   * core=30 对应最大预期并发上限，实际并发由 Semaphore（= concurrency_limit，默认 12）控制。
   * queue=2000 确保所有提交任务都能入队而不触发 AbortPolicy。
   */
  @Bean(name = "faqRecordExecutor")
  public ThreadPoolTaskExecutor faqRecordExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(30);
    executor.setMaxPoolSize(30);
    executor.setQueueCapacity(2000);
    executor.setThreadNamePrefix("faq-record-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }
}
