package com.guoyu.platform.textbook.vo;

/** T3/T4 统计卡（方案 §6.2）：只按当前用户 + active 全量统计，不受筛选/分页影响；各口径可能相交。 */
public record TextbookTaskSummaryVO(
    WritingTaskSummaryVO writing,
    ReviewTaskSummaryVO review) {

  /** T3 编写统计卡。 */
  public record WritingTaskSummaryVO(
      long assignedCount,
      long writingCount,
      long pendingReviewCount,
      long returnedCount,
      long approvedCount) {
  }

  /** T4 审定统计卡。 */
  public record ReviewTaskSummaryVO(
      long pendingCount,
      long returnedCount,
      long partialApprovedCount,
      long approvedCount) {
  }
}
