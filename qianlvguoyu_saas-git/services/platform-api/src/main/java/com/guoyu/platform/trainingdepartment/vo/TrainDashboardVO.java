package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/**
 * 培训数据看板视图对象
 */
public record TrainDashboardVO(
    SummaryKpi summary,
    List<MonthlyTrend> monthlyTrends,
    List<AlertItem> alerts
) {
    public record SummaryKpi(
        double planExecutionRate,
        double materialSubmissionRate,
        double certificationRate,
        double subsidyApprovalRate,
        double trainingCompletionRate,
        long totalProjects,
        long totalStudents,
        long totalCertificates,
        long totalBudgets
    ) {}

    public record MonthlyTrend(
        String month,
        long count
    ) {}

    public record AlertItem(
        String indicator,
        String currentValue,
        String threshold,
        String trend,
        String status,
        String suggestion
    ) {}

    public record MilestoneItem(
        Long projectId,
        String projectName,
        String currentStage,
        Integer progress,
        String companyName
    ) {}
}
