package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.*;
import com.guoyu.platform.trainingdepartment.mapper.*;
import com.guoyu.platform.trainingdepartment.vo.TrainDashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TrainDashboardApplicationService {

  private final TrainProjectMapper projectMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainUserPaperMapper userPaperMapper;
  private final TrainUserPatentMapper userPatentMapper;
  private final TrainBudgetMapper budgetMapper;
  private final TrainSolutionMapper solutionMapper;

  public TrainDashboardApplicationService(TrainProjectMapper projectMapper,
                                     TrainPeopleListMapper peopleListMapper,
                                     TrainUserPaperMapper userPaperMapper,
                                     TrainUserPatentMapper userPatentMapper,
                                     TrainBudgetMapper budgetMapper,
                                     TrainSolutionMapper solutionMapper) {
    this.projectMapper = projectMapper;
    this.peopleListMapper = peopleListMapper;
    this.userPaperMapper = userPaperMapper;
    this.userPatentMapper = userPatentMapper;
    this.budgetMapper = budgetMapper;
    this.solutionMapper = solutionMapper;
  }

  @Transactional(readOnly = true)
  public TrainDashboardVO getDashboard() {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    long totalProjects = projectMapper.selectCount(new LambdaQueryWrapper<TrainProjectEntity>()
        .eq(TrainProjectEntity::getTenantId, tenantId)
        .eq(TrainProjectEntity::getDeleted, 0));

    long completedProjects = projectMapper.selectCount(new LambdaQueryWrapper<TrainProjectEntity>()
        .eq(TrainProjectEntity::getTenantId, tenantId)
        .eq(TrainProjectEntity::getDeleted, 0)
        .eq(TrainProjectEntity::getStatus, 2));

    long totalStudents = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0));

    long totalCertificates = userPaperMapper.selectCount(new LambdaQueryWrapper<TrainUserPaperEntity>()
        .eq(TrainUserPaperEntity::getTenantId, tenantId)
        .eq(TrainUserPaperEntity::getDeleted, 0))
        + userPatentMapper.selectCount(new LambdaQueryWrapper<TrainUserPatentEntity>()
        .eq(TrainUserPatentEntity::getTenantId, tenantId)
        .eq(TrainUserPatentEntity::getDeleted, 0));

    long totalBudgets = budgetMapper.selectCount(new LambdaQueryWrapper<TrainBudgetEntity>()
        .eq(TrainBudgetEntity::getTenantId, tenantId)
        .eq(TrainBudgetEntity::getDeleted, 0));

    double planExecutionRate = totalProjects > 0
        ? Math.round(completedProjects * 10000.0 / totalProjects) / 100.0
        : 0.0;

    // Material submission rate: estimated based on students with records
    long studentsWithRecords = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .isNotNull(TrainPeopleListEntity::getUserId));
    double materialSubmissionRate = totalStudents > 0
        ? Math.round(studentsWithRecords * 10000.0 / totalStudents) / 100.0
        : 0.0;

    double certificationRate = totalStudents > 0
        ? Math.round(totalCertificates * 10000.0 / (totalStudents > 0 ? totalStudents : 1)) / 100.0
        : 0.0;

    // Subsidy approval rate: approved budgets / total
    long approvedBudgets = budgetMapper.selectCount(new LambdaQueryWrapper<TrainBudgetEntity>()
        .eq(TrainBudgetEntity::getTenantId, tenantId)
        .eq(TrainBudgetEntity::getDeleted, 0)
        .eq(TrainBudgetEntity::getStatus, 1));
    double subsidyApprovalRate = totalBudgets > 0
        ? Math.round(approvedBudgets * 10000.0 / totalBudgets) / 100.0
        : 0.0;

    TrainDashboardVO.SummaryKpi summary = new TrainDashboardVO.SummaryKpi(
        planExecutionRate,
        materialSubmissionRate,
        certificationRate,
        subsidyApprovalRate,
        totalProjects,
        totalStudents,
        totalCertificates,
        totalBudgets
    );

    // Monthly trends from solutions (past 6 months)
    List<TrainDashboardVO.MonthlyTrend> monthlyTrends = new ArrayList<>();
    DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("M月");
    for (int i = 5; i >= 0; i--) {
      LocalDateTime start = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
      LocalDateTime end = start.plusMonths(1);
      long count = solutionMapper.selectCount(new LambdaQueryWrapper<TrainSolutionEntity>()
          .eq(TrainSolutionEntity::getTenantId, tenantId)
          .eq(TrainSolutionEntity::getDeleted, 0)
          .ge(TrainSolutionEntity::getCreateTime, start)
          .lt(TrainSolutionEntity::getCreateTime, end));
      monthlyTrends.add(new TrainDashboardVO.MonthlyTrend(start.format(monthFmt), count));
    }

    // Alert items
    List<TrainDashboardVO.AlertItem> alerts = List.of(
        new TrainDashboardVO.AlertItem("补贴申报通过率",
            subsidyApprovalRate + "%", ">=85%",
            subsidyApprovalRate < 85 ? "连续下降" : "稳定",
            subsidyApprovalRate < 85 ? "预警" : "正常",
            subsidyApprovalRate < 85 ? "复核模板版本与附件完整性" : "保持现有审核标准"),
        new TrainDashboardVO.AlertItem("结课材料完整率",
            materialSubmissionRate + "%", ">=95%",
            materialSubmissionRate >= 95 ? "稳定" : "需关注",
            materialSubmissionRate >= 95 ? "正常" : "关注",
            materialSubmissionRate >= 95 ? "保持现有抽检频率" : "增加材料审核力度"),
        new TrainDashboardVO.AlertItem("计划执行率",
            planExecutionRate + "%", ">=90%",
            planExecutionRate >= 90 ? "稳定" : "下降",
            planExecutionRate >= 90 ? "正常" : "关注",
            planExecutionRate >= 90 ? "保持现有推进节奏" : "跟进项目进度管理")
    );

    return new TrainDashboardVO(summary, monthlyTrends, alerts);
  }

  @Transactional(readOnly = true)
  public List<TrainDashboardVO.MilestoneItem> getMilestones() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainProjectEntity> wrapper = new LambdaQueryWrapper<TrainProjectEntity>()
        .eq(TrainProjectEntity::getTenantId, tenantId)
        .eq(TrainProjectEntity::getDeleted, 0)
        .ne(TrainProjectEntity::getStatus, 3) // exclude archived
        .orderByDesc(TrainProjectEntity::getCreatedAt)
        .last("LIMIT 20");
    List<TrainProjectEntity> projects = projectMapper.selectList(wrapper);
    return projects.stream().map(p -> new TrainDashboardVO.MilestoneItem(
        p.getId(),
        p.getProjectName(),
        p.getCurrentStage(),
        p.getProgress(),
        p.getCompanyName()
    )).toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getStageDistribution() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainProjectEntity> wrapper = new LambdaQueryWrapper<TrainProjectEntity>()
        .eq(TrainProjectEntity::getTenantId, tenantId)
        .eq(TrainProjectEntity::getDeleted, 0);
    List<TrainProjectEntity> projects = projectMapper.selectList(wrapper);
    Map<String, Long> distribution = new HashMap<>();
    for (TrainProjectEntity p : projects) {
      String stage = p.getCurrentStage() != null ? p.getCurrentStage() : "未知";
      distribution.merge(stage, 1L, Long::sum);
    }
    return distribution;
  }
}
