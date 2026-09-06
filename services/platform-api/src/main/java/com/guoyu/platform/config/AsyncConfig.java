package com.guoyu.platform.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
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

  /**
   * 理论试题生成批次编排池（每批次占 1 线程做编排，块任务派发到 theoryGenBlockExecutor，暂停只挡新块）。
   * AbortPolicy：同时批次过多时直接拒绝 → 503 服务繁忙（AGENTS.md 坑 #12/#14）。
   */
  @Bean(name = "theoryGenExecutor")
  public ThreadPoolTaskExecutor theoryGenExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("theory-gen-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 理论试题生成块任务池（优化方案 v0.3 P3）：并发度 platform.theory.gen.block-concurrency（默认 5，可配）。
   * core=max=N；queueCapacity=N 吸收"信号量释放→线程就绪"交接抖动避免误拒绝，
   * 在飞 LLM 调用数由派发侧同源信号量（permits=N）保证 ≤ N。
   * 峰值 LLM 并发 = 批次间并发 × 此值，GLM 1302 限流频发时下调配置。
   * AbortPolicy：拒绝由编排侧捕获计块失败（坑 #12/#14）。
   */
  @Bean(name = "theoryGenBlockExecutor")
  public ThreadPoolTaskExecutor theoryGenBlockExecutor(
      @Value("${platform.theory.gen.block-concurrency:5}") int blockConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, blockConcurrency));
    executor.setMaxPoolSize(Math.max(1, blockConcurrency));
    executor.setQueueCapacity(Math.max(1, blockConcurrency));
    executor.setThreadNamePrefix("theory-gen-block-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * AI 同步学鼓励语异步生成池（ReviewLlmAssistService）。
   * AbortPolicy：队列满时拒绝，由调用方捕获降级为预置文案（AGENTS.md 坑 #12/#14）。
   * （单库化前名为 reviewSyncExecutor，兼作 exam_result 异步回写池；回写已改事务内同步直写）
   */
  @Bean(name = "reviewLlmAssistExecutor")
  public ThreadPoolTaskExecutor reviewLlmAssistExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("review-sync-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(name = "assessorLedgerImportExecutor")
  public ThreadPoolTaskExecutor assessorLedgerImportExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(5);
    executor.setThreadNamePrefix("assessor-ledger-import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(name = "certImportExecutor")
  public ThreadPoolTaskExecutor certImportExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 预览匹配：PDF 解析 / OCR 可并行，提速批量证书导入
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("cert-import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
