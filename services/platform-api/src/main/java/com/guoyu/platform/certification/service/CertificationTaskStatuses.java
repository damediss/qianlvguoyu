package com.guoyu.platform.certification.service;

import java.util.Set;

/**
 * 任务状态（certification_task.status）枚举。状态与阶段（current_phase）解耦：
 * 状态描述阶段内的工作进度（待补充完整/进行中/待审核/已驳回/已完成），阶段描述任务所处的大阶段。
 *
 * @see CertificationTaskPhases
 */
final class CertificationTaskStatuses {

  /** 新建后已进入认定筹备，但任务创建阶段五份资料尚未补齐归档。 */
  static final String PENDING_COMPLETE = "pending_complete";
  static final String IN_PROGRESS = "in_progress";
  static final String PENDING_REVIEW = "pending_review";
  static final String REJECTED = "rejected";
  static final String COMPLETED = "completed";

  static final Set<String> STATUSES = Set.of(
      PENDING_COMPLETE, IN_PROGRESS, PENDING_REVIEW, REJECTED, COMPLETED
  );

  static String statusName(String status) {
    return switch (status == null ? "" : status) {
      case PENDING_COMPLETE -> "待补充完整";
      case IN_PROGRESS -> "进行中";
      case PENDING_REVIEW -> "待审核";
      case REJECTED -> "已驳回";
      case COMPLETED -> "已完成";
      default -> status;
    };
  }

  static boolean isValid(String status) {
    return status != null && STATUSES.contains(status);
  }

  private CertificationTaskStatuses() {
  }
}
