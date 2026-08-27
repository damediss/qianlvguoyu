package com.guoyu.platform.exam.reviewsync.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * AI 同步学定时任务（§8）。
 *
 * <p>线程边界（AGENTS.md 坑 #15/#17）：job 线程入口显式
 * {@code TenantContextHolder.setTenantId(DEFAULT_TENANT_ID=10159)} + finally clear——MyBatis-Plus 多租户拦截器
 * 依赖该上下文；job 内不调 SecurityContext。
 *
 * <p>单库化：exam_result 已改作答事务内同步直写（ExamResultWriteRepository），原 5min 回写补偿 job 拆除。
 * 开关：{@code platform.review-sync.jobs-enabled=false} 时整体不装配（application.yml 默认 false，
 * 仅生产服务器 env 显式 {@code REVIEW_SYNC_JOBS_ENABLED=true} 开启；测试环境 src/test/resources 已关闭）。
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "platform.review-sync", name = "jobs-enabled", havingValue = "true", matchIfMissing = true)
public class ReviewSyncScheduler {

  private final ReviewPlanApplicationService reviewService;

  public ReviewSyncScheduler(ReviewPlanApplicationService reviewService) {
    this.reviewService = reviewService;
  }

  /** 每日 04:00：遍历 active 计划，中断检测 → 物化当日任务 → 异步鼓励语 */
  @Scheduled(cron = "${platform.review-sync.daily-cron:0 0 4 * * *}")
  public void dailyMaterialize() {
    runWithTenant(() -> {
      int done = reviewService.materializeAllActivePlans(LocalDate.now());
      if (done > 0) {
        log.info("[AI复习] 每日物化完成：处理计划 {} 个", done);
      }
    });
  }

  /** 周日 05:00：错题一致性复核（兜底） */
  @Scheduled(cron = "${platform.review-sync.weekly-cron:0 0 5 * * SUN}")
  public void weeklyCleanup() {
    runWithTenant(reviewService::weeklyCleanup);
  }

  private void runWithTenant(Runnable runnable) {
    try {
      TenantContextHolder.setTenantId(TenantContextHolder.DEFAULT_TENANT_ID);
      runnable.run();
    } catch (Exception e) {
      log.error("[AI复习] 定时任务异常：{}", e.getMessage(), e);
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void runWithTenant(java.util.function.Supplier<Object> supplier) {
    runWithTenant(supplier::get);
  }
}
