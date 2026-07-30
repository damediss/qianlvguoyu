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
import java.util.LinkedHashMap;
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
  private final TrainInfoListMapper infoListMapper;

  public TrainDashboardApplicationService(TrainProjectMapper projectMapper,
                                     TrainPeopleListMapper peopleListMapper,
                                     TrainUserPaperMapper userPaperMapper,
                                     TrainUserPatentMapper userPatentMapper,
                                     TrainBudgetMapper budgetMapper,
                                     TrainSolutionMapper solutionMapper,
                                     TrainInfoListMapper infoListMapper) {
    this.projectMapper = projectMapper;
    this.peopleListMapper = peopleListMapper;
    this.userPaperMapper = userPaperMapper;
    this.userPatentMapper = userPatentMapper;
    this.budgetMapper = budgetMapper;
    this.solutionMapper = solutionMapper;
    this.infoListMapper = infoListMapper;
  }

  @Transactional(readOnly = true)
  public TrainDashboardVO getDashboard() {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    // 培训项目：改用 train_solution 口径（审批通过 status=2 视为已立项，
    // 总数=审批通过的方案数；执行率=审批通过且对应培训信息已毕业的比例）
    long totalProjects = solutionMapper.selectCount(new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0)
        .eq(TrainSolutionEntity::getStatus, 2));

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

    // 培训完成率：已毕业班级数(train_info_list.status=2) / 总培训信息数
    long totalTrainInfos = infoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0));
    long graduatedTrainInfos = infoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .eq(TrainInfoListEntity::getStatus, 2));
    double trainingCompletionRate = totalTrainInfos > 0
        ? Math.round(graduatedTrainInfos * 10000.0 / totalTrainInfos) / 100.0
        : 0.0;

    // 计划执行率：审批通过的方案中，对应培训信息已毕业的比例
    long executedProjects = 0;
    if (totalProjects > 0) {
      List<TrainSolutionEntity> approvedSolutions = solutionMapper.selectList(
          new LambdaQueryWrapper<TrainSolutionEntity>()
              .eq(TrainSolutionEntity::getTenantId, tenantId)
              .eq(TrainSolutionEntity::getDeleted, 0)
              .eq(TrainSolutionEntity::getStatus, 2)
              .isNotNull(TrainSolutionEntity::getTrainInfoId));
      if (!approvedSolutions.isEmpty()) {
        List<Long> infoIds = approvedSolutions.stream()
            .map(TrainSolutionEntity::getTrainInfoId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        if (!infoIds.isEmpty()) {
          executedProjects = infoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
              .eq(TrainInfoListEntity::getTenantId, tenantId)
              .eq(TrainInfoListEntity::getDeleted, 0)
              .eq(TrainInfoListEntity::getStatus, 2)
              .in(TrainInfoListEntity::getId, infoIds));
        }
      }
    }
    double planExecutionRate = totalProjects > 0
        ? Math.round(executedProjects * 10000.0 / totalProjects) / 100.0
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
        trainingCompletionRate,
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
            planExecutionRate >= 90 ? "保持现有推进节奏" : "跟进项目进度管理"),
        new TrainDashboardVO.AlertItem("培训完成率",
            trainingCompletionRate + "%", ">=80%",
            trainingCompletionRate >= 80 ? "稳定" : "下降",
            trainingCompletionRate >= 80 ? "正常" : "关注",
            trainingCompletionRate >= 80 ? "保持现有毕业推进节奏" : "推进待毕业班级流程")
    );

    return new TrainDashboardVO(summary, monthlyTrends, alerts);
  }

  @Transactional(readOnly = true)
  public List<TrainDashboardVO.MilestoneItem> getMilestones() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // 改用 train_solution 口径：审批通过的方案作为里程碑，currentStage 取对应培训信息状态
    LambdaQueryWrapper<TrainSolutionEntity> wrapper = new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0)
        .eq(TrainSolutionEntity::getStatus, 2)
        .orderByDesc(TrainSolutionEntity::getCreateTime)
        .last("LIMIT 20");
    List<TrainSolutionEntity> solutions = solutionMapper.selectList(wrapper);

    if (solutions.isEmpty()) {
      return List.of();
    }

    List<Long> infoIds = solutions.stream()
        .map(TrainSolutionEntity::getTrainInfoId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, TrainInfoListEntity> infoMap = new LinkedHashMap<>();
    if (!infoIds.isEmpty()) {
      infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
              .eq(TrainInfoListEntity::getTenantId, tenantId)
              .eq(TrainInfoListEntity::getDeleted, 0)
              .in(TrainInfoListEntity::getId, infoIds))
          .forEach(info -> infoMap.put(info.getId(), info));
    }

    return solutions.stream().map(s -> {
      TrainInfoListEntity info = s.getTrainInfoId() != null ? infoMap.get(s.getTrainInfoId()) : null;
      String stage = resolveStage(info);
      int progress = resolveProgress(info);
      String companyName = info != null && info.getCompanyName() != null ? info.getCompanyName() : s.getCompanyName();
      return new TrainDashboardVO.MilestoneItem(
          s.getId(),
          s.getTitle() != null ? s.getTitle() : (info != null ? info.getTitle() : ""),
          stage,
          progress,
          companyName != null ? companyName : ""
      );
    }).toList();
  }

  private static String resolveStage(TrainInfoListEntity info) {
    if (info == null || info.getStatus() == null) {
      return "pending";
    }
    return switch (info.getStatus()) {
      case 0, 1 -> "in_progress";
      case 2 -> "completed";
      default -> "pending";
    };
  }

  private static int resolveProgress(TrainInfoListEntity info) {
    if (info == null || info.getStatus() == null) {
      return 0;
    }
    return switch (info.getStatus()) {
      case 0 -> 10;
      case 1 -> 50;
      case 2 -> 100;
      default -> 0;
    };
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getStageDistribution() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // 阶段分布改用 train_info_list.status 聚合（更贴合实际培训流程）
    List<TrainInfoListEntity> infos = infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0));
    Map<String, Long> distribution = new LinkedHashMap<>();
    for (TrainInfoListEntity info : infos) {
      String stage = resolveStage(info);
      distribution.merge(stage, 1L, Long::sum);
    }
    return distribution;
  }
}
