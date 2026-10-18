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

  /**
   * AI 课件生成编排池(方案 v1.2 §11):每个项目占 1 线程跑"解析→消化→大纲"或页级派发编排。
   * 并发项目上限 platform.courseware.batch-concurrency(默认 2)。
   * AbortPolicy:同时项目过多直接拒绝 → 503 服务繁忙(AGENTS.md 坑 #12/#14)。
   */
  @Bean(name = "coursewareExecutor")
  public ThreadPoolTaskExecutor coursewareExecutor(
      @Value("${platform.courseware.batch-concurrency:2}") int batchConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, batchConcurrency));
    executor.setMaxPoolSize(Math.max(1, batchConcurrency));
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("courseware-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * AI 课件页级生成池(方案 v1.2 §11):core=4/max=8/queue=50。
   * 页级并发远小于 glm-5.3-flash 全局闸口(FaqLlmClient 信号量默认 50),由闸口统一限流。
   * AbortPolicy:队列耗尽拒绝由编排侧捕获计页失败(坑 #12/#14)。
   */
  @Bean(name = "coursewareSlideExecutor")
  public ThreadPoolTaskExecutor coursewareSlideExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("courseware-slide-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
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
   * 并发批次上限 platform.theory.gen.batch-concurrency（默认 3，可配）：同蓝图仍串行（提交侧校验），
   * 不同蓝图可并行；排队超出部分显示"等待中"。
   * AbortPolicy：同时批次过多时直接拒绝 → 503 服务繁忙（AGENTS.md 坑 #12/#14）。
   */
  @Bean(name = "theoryGenExecutor")
  public ThreadPoolTaskExecutor theoryGenExecutor(
      @Value("${platform.theory.gen.batch-concurrency:3}") int batchConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(batchConcurrency);
    executor.setMaxPoolSize(batchConcurrency);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("theory-gen-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 理论试题生成块任务池（优化方案 v0.3 P3；2026-09-08 GLM 并发闸口方案 A 配套扩容）。
   * core=max=platform.theory.gen.block-concurrency（默认 50，可配）；queueCapacity 固定 200：
   * 3 批次并行 × 每批 50 派发名额 = 150 个已派发块全部可入队，不再 AbortPolicy 误拒计失败。
   * glm-5.3-flash 在飞调用数由 FaqLlmClient 模型闸口（platform.ai.glm-max-concurrency，默认 50）
   * 全局统一封顶、超出排队等待；本池只是搬运线程，线程数 ≥ 闸口上限才能跑满并发。
   * AbortPolicy：队列耗尽的极端情况拒绝由编排侧捕获计块失败（坑 #12/#14）。
   */
  @Bean(name = "theoryGenBlockExecutor")
  public ThreadPoolTaskExecutor theoryGenBlockExecutor(
      @Value("${platform.theory.gen.block-concurrency:50}") int blockConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, blockConcurrency));
    executor.setMaxPoolSize(Math.max(1, blockConcurrency));
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("theory-gen-block-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 操作技能试题生成批次编排池（每批次占 1 线程做编排，块任务派发到 skillGenBlockExecutor，暂停只挡新块）。
   * 并发批次上限 platform.skill.gen.batch-concurrency（默认 2）：同蓝图仍串行（提交侧校验），
   * 不同蓝图可并行。AbortPolicy：批次过多直接拒绝 → 503 服务繁忙（AGENTS.md 坑 #12/#14）。
   */
  @Bean(name = "skillGenExecutor")
  public ThreadPoolTaskExecutor skillGenExecutor(
      @Value("${platform.skill.gen.batch-concurrency:2}") int batchConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(batchConcurrency);
    executor.setMaxPoolSize(batchConcurrency);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("skill-gen-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 操作技能试题生成块任务池（1 考评点 = 1 块，产出 5 题三件套，单块输出体量大）。
   * core=max=platform.skill.gen.block-concurrency（默认 5，可配）；queueCapacity 固定 60
   * （2 批次并行 × 每批 5 个派发名额 = 10 个已派发块全部可入队，极端情况由编排侧捕获计块失败）。
   * glm-5.3-flash 在飞调用仍受 FaqLlmClient 模型闸口（默认 50）全局封顶；本池只是搬运线程。
   */
  @Bean(name = "skillGenBlockExecutor")
  public ThreadPoolTaskExecutor skillGenBlockExecutor(
      @Value("${platform.skill.gen.block-concurrency:5}") int blockConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, blockConcurrency));
    executor.setMaxPoolSize(Math.max(1, blockConcurrency));
    executor.setQueueCapacity(60);
    executor.setThreadNamePrefix("skill-gen-block-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 知识归属修正批次编排池（方案 v1.2 §7.3）：每批次占 1 线程做编排，单行分类任务派发到
   * knowledgeFixLlmExecutor；AbortPolicy 拒绝 → 503（坑 #12/#14）。
   */
  @Bean(name = "knowledgeFixExecutor")
  public ThreadPoolTaskExecutor knowledgeFixExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("knowledge-fix-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 知识归属修正单行分类池（方案 v1.2 §7.3）：并发度 platform.knowledgefix.llm-concurrency（默认 5）。
   * 派发侧信号量同源限流；AbortPolicy 拒绝由编排侧捕获计失败（坑 #12/#14）。
   */
  @Bean(name = "knowledgeFixLlmExecutor")
  public ThreadPoolTaskExecutor knowledgeFixLlmExecutor(
      @Value("${platform.knowledgefix.llm-concurrency:5}") int llmConcurrency) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, llmConcurrency));
    executor.setMaxPoolSize(Math.max(1, llmConcurrency));
    executor.setQueueCapacity(Math.max(5, llmConcurrency));
    executor.setThreadNamePrefix("knowledge-fix-llm-");
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

  /**
   * AI 同步学模考主观题并行判分池（《简答题作答与AI判分方案》§3.4 mock 三段式②）：
   * 每道主观题 1 次 LLM 判分（单题超时 20s + 失败重试 1 次由编排侧控制）。
   * core=max=5：单卷主观题上限 ai_review_mock_subjective_max 默认 5，整卷主观题可全部并行；
   * queue=25 缓冲多学员并发交卷。
   * AbortPolicy：队列满拒绝由编排侧捕获、该题按 0 分兜底并标注（踩坑 #12——禁用
   * CallerRunsPolicy，否则拒绝时退化为 HTTP 线程同步判分，前端"卡死"；踩坑 #14——@Bean
   * 返回类型必须 ThreadPoolTaskExecutor + 注入端 @Qualifier，避免多 Executor Bean 注入歧义）。
   */
  @Bean(name = "aiGradeMockExecutor")
  public ThreadPoolTaskExecutor aiGradeMockExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(25);
    executor.setThreadNamePrefix("ai-grade-mock-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
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
  public ThreadPoolTaskExecutor certImportExecutor() {    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
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

  /**
   * 历年真题异步批量导入池（方案 §2.9）：core=max=2，queue=20。
   * AbortPolicy：队列满直接拒绝 → 调用方标记导入记录失败并返回 503（坑 #12/#14）。
   */
  @Bean(name = "zhentiImportExecutor")
  public ThreadPoolTaskExecutor zhentiImportExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("zhenti-import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(300);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }
}
