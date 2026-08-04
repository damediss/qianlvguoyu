package com.guoyu.platform.integration.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 同步告警（鉴权方案 §3.5.3：连续失败 20 周期 ≈ 1 分钟发告警）。
 *
 * <p>当前项目无邮件/Webhook 告警通道（侦察结论），统一以 {@code [SYNC-ALERT]} error 级日志输出，
 * 供日志采集/值守检索；后续接入通知渠道时仅需改本类。
 */
@Slf4j
@Component
public class SyncAlertService {

  /** 快周期失败上报：达阈值 error 告警，未达阈值 warn */
  public void cycleFailed(String table, int consecutiveFails, int threshold, String errorMessage) {
    if (consecutiveFails >= threshold) {
      log.error("[SYNC-ALERT] 同步任务连续失败 table={} consecutiveFails={} threshold={} error={}",
          table, consecutiveFails, threshold, errorMessage);
    } else {
      log.warn("[SYNC] 同步周期失败 table={} consecutiveFails={} error={}", table, consecutiveFails, errorMessage);
    }
  }

  /** 每日全量对账漂移上报（修复后仍不一致 = 大闸告警） */
  public void drift(String detail) {
    log.error("[SYNC-ALERT] 全量对账漂移: {}", detail);
  }
}
