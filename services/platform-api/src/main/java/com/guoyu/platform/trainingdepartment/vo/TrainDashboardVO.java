package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/**
 * 培训数据看板视图对象
 */
public record TrainDashboardVO(
    SummaryKpi summary,
    List<MonthlyTrend> monthlyTrends,
    List<AlertItem> alerts,
    List<ModuleStat> modules,
    List<CompanyOverview> companies
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
        long totalBudgets,
        long totalClasses,
        long activeClasses,
        long totalExams,
        long totalIssuedCerts,
        long totalSubsidies,
        long inProgressTrainings,
        long totalCompanies,
        long activeCompanies
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

    /** 培训全生命周期模块统计 */
    public record ModuleStat(
        String code,
        String name,
        long count,
        String hint
    ) {}

    /** 企业培训概览 */
    public record CompanyOverview(
        Long companyId,
        String companyName,
        long projectCount,
        long trainingCount,
        long pendingCount,
        long inProgressCount,
        long completedCount,
        long classCount,
        long activeClassCount,
        long studentCount,
        long examCount,
        long issuedCertCount,
        long subsidyCount,
        List<CompanyTrainingItem> trainings
    ) {}

    /** 企业下单次开班/培训明细 */
    public record CompanyTrainingItem(
        Long trainInfoId,
        String title,
        String stage,
        String startDate,
        String endDate,
        long classCount,
        long studentCount,
        long examCount,
        long issuedCertCount
    ) {}
}
