package com.guoyu.platform.certification.vo;

import java.time.LocalDate;
import java.util.List;

public record CertificationDashboardVO(
    Summary summary,
    QualitySummary quality,
    FilterOptions filterOptions,
    List<DistributionItem> phaseDistribution,
    List<DistributionItem> workTypeDistribution,
    List<DistributionItem> levelDistribution,
    List<WorkTypeLevelItem> workTypeLevelDistribution,
    List<MonthlyTrendItem> monthlyTrends,
    List<ResourceItem> resources,
    List<AlertItem> riskAlerts,
    List<AlertItem> complianceAlerts
) {

  public record Summary(
      long totalTasks,
      long inProgressTasks,
      long completedTasks,
      double taskCompletionRate,
      long plannedPeopleCount,
      long actualPeopleCount,
      long overdueTasks,
      double averageCycleDays,
      long archivedTasks,
      long pendingArchiveTasks,
      double archiveCompletionRate,
      double taskMaterialCompletionRate
  ) {
  }

  public record QualitySummary(
      long openRiskCount,
      long highRiskTaskCount,
      double checklistCompletionRate,
      double candidateMaterialCompletionRate,
      long importErrorCount
  ) {
  }

  public record FilterOptions(
      List<String> certificationTypes,
      List<String> workTypes,
      List<String> levels
  ) {
  }

  public record DistributionItem(
      String code,
      String name,
      long taskCount,
      long peopleCount
  ) {
  }

  public record WorkTypeLevelItem(
      String workTypeName,
      String levelName,
      long peopleCount
  ) {
  }

  public record MonthlyTrendItem(
      String month,
      long createdTaskCount,
      long archivedTaskCount,
      long archivedPeopleCount
  ) {
  }

  public record ResourceItem(
      String code,
      String name,
      long total,
      long available,
      long busy,
      long disabled,
      long expiring,
      long capacity
  ) {
  }

  public record AlertItem(
      Long taskId,
      String taskNo,
      String taskName,
      String alertType,
      String level,
      String description,
      LocalDate dueDate
  ) {
  }
}
