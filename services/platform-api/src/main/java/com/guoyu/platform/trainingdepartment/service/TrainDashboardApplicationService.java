package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainBudgetEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSolutionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainUserPaperEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainUserPatentEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainBudgetMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSolutionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserPaperMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserPatentMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainDashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TrainDashboardApplicationService {

  private final TrainPeopleListMapper peopleListMapper;
  private final TrainUserPaperMapper userPaperMapper;
  private final TrainUserPatentMapper userPatentMapper;
  private final TrainBudgetMapper budgetMapper;
  private final TrainSolutionMapper solutionMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainInfoClassMapper infoClassMapper;
  private final TrainExamListMapper examListMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainSubsidyMapper subsidyMapper;
  private final OrgMapper orgMapper;

  public TrainDashboardApplicationService(TrainPeopleListMapper peopleListMapper,
                                     TrainUserPaperMapper userPaperMapper,
                                     TrainUserPatentMapper userPatentMapper,
                                     TrainBudgetMapper budgetMapper,
                                     TrainSolutionMapper solutionMapper,
                                     TrainInfoListMapper infoListMapper,
                                     TrainInfoClassMapper infoClassMapper,
                                     TrainExamListMapper examListMapper,
                                     TrainCertificateMapper certificateMapper,
                                     TrainSubsidyMapper subsidyMapper,
                                     OrgMapper orgMapper) {
    this.peopleListMapper = peopleListMapper;
    this.userPaperMapper = userPaperMapper;
    this.userPatentMapper = userPatentMapper;
    this.budgetMapper = budgetMapper;
    this.solutionMapper = solutionMapper;
    this.infoListMapper = infoListMapper;
    this.infoClassMapper = infoClassMapper;
    this.examListMapper = examListMapper;
    this.certificateMapper = certificateMapper;
    this.subsidyMapper = subsidyMapper;
    this.orgMapper = orgMapper;
  }

  @Transactional(readOnly = true)
  public TrainDashboardVO getDashboard() {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    List<TrainDashboardVO.CompanyOverview> companies = buildCompanyOverviews(tenantId);
    long totalCompanies = companies.size();
    long activeCompanies = companies.stream().filter(c -> c.trainingCount() > 0 || c.projectCount() > 0).count();

    long totalProjects = solutionMapper.selectCount(new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0)
        .eq(TrainSolutionEntity::getStatus, 2));

    long pendingSolutions = solutionMapper.selectCount(new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0)
        .in(TrainSolutionEntity::getStatus, 0, 1));

    long totalStudents = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0));

    long totalCertificates = userPaperMapper.selectCount(new LambdaQueryWrapper<TrainUserPaperEntity>()
        .eq(TrainUserPaperEntity::getTenantId, tenantId)
        .eq(TrainUserPaperEntity::getDeleted, 0))
        + userPatentMapper.selectCount(new LambdaQueryWrapper<TrainUserPatentEntity>()
        .eq(TrainUserPatentEntity::getTenantId, tenantId)
        .eq(TrainUserPatentEntity::getDeleted, 0));

    long totalIssuedCerts = certificateMapper.selectCount(new LambdaQueryWrapper<TrainCertificateEntity>()
        .eq(TrainCertificateEntity::getTenantId, tenantId)
        .eq(TrainCertificateEntity::getDeleted, 0));

    long totalBudgets = budgetMapper.selectCount(new LambdaQueryWrapper<TrainBudgetEntity>()
        .eq(TrainBudgetEntity::getTenantId, tenantId)
        .eq(TrainBudgetEntity::getDeleted, 0));

    long totalSubsidies = subsidyMapper.selectCount(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getDeleted, 0));

    long totalExams = examListMapper.selectCount(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getDeleted, 0));

    List<Long> tenantInfoIds = infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getTenantId, tenantId)
            .eq(TrainInfoListEntity::getDeleted, 0)
            .select(TrainInfoListEntity::getId))
        .stream()
        .map(TrainInfoListEntity::getId)
        .toList();

    long totalClasses = 0;
    long activeClasses = 0;
    if (!tenantInfoIds.isEmpty()) {
      totalClasses = infoClassMapper.selectCount(new LambdaQueryWrapper<TrainInfoClassEntity>()
          .eq(TrainInfoClassEntity::getDeleted, 0)
          .in(TrainInfoClassEntity::getTrainInfoId, tenantInfoIds));
      activeClasses = infoClassMapper.selectCount(new LambdaQueryWrapper<TrainInfoClassEntity>()
          .eq(TrainInfoClassEntity::getDeleted, 0)
          .in(TrainInfoClassEntity::getTrainInfoId, tenantInfoIds)
          .and(w -> w.isNull(TrainInfoClassEntity::getGraduateStatus)
              .or()
              .ne(TrainInfoClassEntity::getGraduateStatus, "done")));
    }

    long totalTrainInfos = tenantInfoIds.size();
    long graduatedTrainInfos = infoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .eq(TrainInfoListEntity::getStatus, 2));
    long inProgressTrainings = infoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .eq(TrainInfoListEntity::getStatus, 1));
    double trainingCompletionRate = totalTrainInfos > 0
        ? Math.round(graduatedTrainInfos * 10000.0 / totalTrainInfos) / 100.0
        : 0.0;

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

    long studentsWithRecords = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .isNotNull(TrainPeopleListEntity::getUserId));
    double materialSubmissionRate = totalStudents > 0
        ? Math.round(studentsWithRecords * 10000.0 / totalStudents) / 100.0
        : 0.0;

    double certificationRate = totalStudents > 0
        ? Math.round(totalIssuedCerts * 10000.0 / totalStudents) / 100.0
        : 0.0;

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
        totalBudgets,
        totalClasses,
        activeClasses,
        totalExams,
        totalIssuedCerts,
        totalSubsidies,
        inProgressTrainings,
        totalCompanies,
        activeCompanies
    );

    List<TrainDashboardVO.ModuleStat> modules = List.of(
        new TrainDashboardVO.ModuleStat("companies", "参训企业", activeCompanies, "有培训/方案的企业"),
        new TrainDashboardVO.ModuleStat("solutions", "方案立项", totalProjects, "审批通过方案"),
        new TrainDashboardVO.ModuleStat("pending_solutions", "方案待审", pendingSolutions, "草稿/审核中"),
        new TrainDashboardVO.ModuleStat("classes", "开班班级", totalClasses, "培训班级总数"),
        new TrainDashboardVO.ModuleStat("active_classes", "在培班级", activeClasses, "未结业班级"),
        new TrainDashboardVO.ModuleStat("students", "学员花名册", totalStudents, "在册学员"),
        new TrainDashboardVO.ModuleStat("exams", "考试计划", totalExams, "跟班考试"),
        new TrainDashboardVO.ModuleStat("certs", "已发证书", totalIssuedCerts, "已发放证书")
    );

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

    return new TrainDashboardVO(summary, monthlyTrends, alerts, modules, companies);
  }

  @Transactional(readOnly = true)
  public List<TrainDashboardVO.CompanyOverview> getCompanyOverviews() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return buildCompanyOverviews(tenantId);
  }

  private List<TrainDashboardVO.CompanyOverview> buildCompanyOverviews(Long tenantId) {
    Map<Long, String> registeredNames = new LinkedHashMap<>();
    orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
            .eq(OrgEntity::getTenantId, tenantId)
            .eq(OrgEntity::getDeleted, 0)
            .eq(OrgEntity::getOrgType, "company")
            .orderByAsc(OrgEntity::getSortOrder)
            .orderByAsc(OrgEntity::getId))
        .forEach(org -> registeredNames.put(org.getId(), org.getName()));

    List<TrainInfoListEntity> infos = infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .orderByDesc(TrainInfoListEntity::getId));

    List<TrainSolutionEntity> solutions = solutionMapper.selectList(new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getDeleted, 0));

    Map<Long, List<TrainInfoListEntity>> infosByCompany = new HashMap<>();
    Map<Long, String> nameHints = new HashMap<>(registeredNames);
    for (TrainInfoListEntity info : infos) {
      Long companyId = normalizeCompanyId(info.getCompanyId());
      if (companyId == null) {
        continue;
      }
      infosByCompany.computeIfAbsent(companyId, k -> new ArrayList<>()).add(info);
      if (StringUtils.hasText(info.getCompanyName())) {
        nameHints.putIfAbsent(companyId, info.getCompanyName().trim());
      }
    }
    for (TrainSolutionEntity solution : solutions) {
      Long companyId = normalizeCompanyId(solution.getCompanyId());
      if (companyId == null) {
        continue;
      }
      if (StringUtils.hasText(solution.getCompanyName())) {
        nameHints.putIfAbsent(companyId, solution.getCompanyName().trim());
      }
    }

    Set<Long> companyIds = new HashSet<>();
    companyIds.addAll(registeredNames.keySet());
    companyIds.addAll(infosByCompany.keySet());
    solutions.stream()
        .map(s -> normalizeCompanyId(s.getCompanyId()))
        .filter(Objects::nonNull)
        .forEach(companyIds::add);

    Map<Long, Long> companyByInfoId = new HashMap<>();
    for (TrainInfoListEntity info : infos) {
      Long companyId = normalizeCompanyId(info.getCompanyId());
      if (companyId != null) {
        companyByInfoId.put(info.getId(), companyId);
      }
    }

    List<Long> infoIds = infos.stream().map(TrainInfoListEntity::getId).filter(Objects::nonNull).toList();
    Map<Long, List<TrainInfoClassEntity>> classesByInfo = new HashMap<>();
    Map<Long, Long> companyByClassId = new HashMap<>();
    if (!infoIds.isEmpty()) {
      List<TrainInfoClassEntity> classes = infoClassMapper.selectList(new LambdaQueryWrapper<TrainInfoClassEntity>()
          .eq(TrainInfoClassEntity::getDeleted, 0)
          .in(TrainInfoClassEntity::getTrainInfoId, infoIds));
      for (TrainInfoClassEntity clazz : classes) {
        classesByInfo.computeIfAbsent(clazz.getTrainInfoId(), k -> new ArrayList<>()).add(clazz);
        Long companyId = normalizeCompanyId(clazz.getCompanyId());
        if (companyId == null) {
          companyId = companyByInfoId.get(clazz.getTrainInfoId());
        }
        if (companyId != null) {
          companyByClassId.put(clazz.getId(), companyId);
        }
      }
    }

    Map<Long, Long> studentByTrainId = new HashMap<>();
    Map<Long, Long> studentByCompany = new HashMap<>();
    List<TrainPeopleListEntity> people = peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .select(TrainPeopleListEntity::getId, TrainPeopleListEntity::getTrainId, TrainPeopleListEntity::getTrainClassId));
    for (TrainPeopleListEntity person : people) {
      if (person.getTrainId() != null) {
        studentByTrainId.merge(person.getTrainId(), 1L, Long::sum);
      }
      Long companyId = null;
      if (person.getTrainClassId() != null) {
        companyId = companyByClassId.get(person.getTrainClassId());
      }
      if (companyId == null && person.getTrainId() != null) {
        companyId = companyByInfoId.get(person.getTrainId());
      }
      if (companyId != null) {
        studentByCompany.merge(companyId, 1L, Long::sum);
      }
    }

    Map<Long, Long> examByTrainId = new HashMap<>();
    Map<Long, Long> examByCompany = new HashMap<>();
    List<TrainExamListEntity> exams = examListMapper.selectList(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getDeleted, 0)
        .select(TrainExamListEntity::getId, TrainExamListEntity::getTrainId));
    for (TrainExamListEntity exam : exams) {
      if (exam.getTrainId() != null) {
        examByTrainId.merge(exam.getTrainId(), 1L, Long::sum);
        Long companyId = companyByInfoId.get(exam.getTrainId());
        if (companyId != null) {
          examByCompany.merge(companyId, 1L, Long::sum);
        }
      }
    }

    Map<Long, Long> certByClassId = new HashMap<>();
    Map<Long, Long> certByCompany = new HashMap<>();
    List<TrainCertificateEntity> certificates = certificateMapper.selectList(new LambdaQueryWrapper<TrainCertificateEntity>()
        .eq(TrainCertificateEntity::getTenantId, tenantId)
        .eq(TrainCertificateEntity::getDeleted, 0)
        .select(TrainCertificateEntity::getId, TrainCertificateEntity::getClassId));
    for (TrainCertificateEntity cert : certificates) {
      if (cert.getClassId() == null) {
        continue;
      }
      certByClassId.merge(cert.getClassId(), 1L, Long::sum);
      Long companyId = companyByClassId.get(cert.getClassId());
      if (companyId != null) {
        certByCompany.merge(companyId, 1L, Long::sum);
      }
    }

    Map<Long, Long> subsidyByCompany = new HashMap<>();
    List<TrainSubsidyEntity> subsidies = subsidyMapper.selectList(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getDeleted, 0)
        .select(TrainSubsidyEntity::getId, TrainSubsidyEntity::getClassId));
    for (TrainSubsidyEntity subsidy : subsidies) {
      if (subsidy.getClassId() == null) {
        continue;
      }
      Long companyId = companyByClassId.get(subsidy.getClassId());
      if (companyId != null) {
        subsidyByCompany.merge(companyId, 1L, Long::sum);
      }
    }

    Map<Long, Long> projectByCompany = new HashMap<>();
    for (TrainSolutionEntity solution : solutions) {
      if (!Objects.equals(solution.getStatus(), 2)) {
        continue;
      }
      Long companyId = normalizeCompanyId(solution.getCompanyId());
      if (companyId != null) {
        projectByCompany.merge(companyId, 1L, Long::sum);
      }
    }

    List<TrainDashboardVO.CompanyOverview> result = new ArrayList<>();
    for (Long companyId : companyIds) {
      List<TrainInfoListEntity> companyInfos = infosByCompany.getOrDefault(companyId, List.of());
      List<TrainDashboardVO.CompanyTrainingItem> trainings = new ArrayList<>();
      long pending = 0;
      long inProgress = 0;
      long completed = 0;
      long classCount = 0;
      long activeClassCount = 0;
      long studentCount = studentByCompany.getOrDefault(companyId, 0L);
      long examCount = examByCompany.getOrDefault(companyId, 0L);
      long issuedCertCount = certByCompany.getOrDefault(companyId, 0L);
      long subsidyCount = subsidyByCompany.getOrDefault(companyId, 0L);

      for (TrainInfoListEntity info : companyInfos) {
        List<TrainInfoClassEntity> infoClasses = classesByInfo.getOrDefault(info.getId(), List.of());
        long infoClassCount = infoClasses.size();
        long infoActiveClasses = infoClasses.stream().filter(c -> !"done".equals(c.getGraduateStatus())).count();
        long infoStudentCount = studentByTrainId.getOrDefault(info.getId(), 0L);
        long infoExamCount = examByTrainId.getOrDefault(info.getId(), 0L);
        long infoCertCount = infoClasses.stream()
            .mapToLong(c -> certByClassId.getOrDefault(c.getId(), 0L))
            .sum();

        classCount += infoClassCount;
        activeClassCount += infoActiveClasses;
        String stage = resolveStageLabel(info);
        switch (stage) {
          case "培训中" -> inProgress++;
          case "已结业" -> completed++;
          default -> pending++;
        }

        trainings.add(new TrainDashboardVO.CompanyTrainingItem(
            info.getId(),
            info.getTitle() != null ? info.getTitle() : "",
            stage,
            info.getStartDate() != null ? info.getStartDate() : "",
            info.getEndDate() != null ? info.getEndDate() : "",
            infoClassCount,
            infoStudentCount,
            infoExamCount,
            infoCertCount
        ));
      }

      String companyName = nameHints.getOrDefault(companyId, "企业#" + companyId);
      result.add(new TrainDashboardVO.CompanyOverview(
          companyId,
          companyName,
          projectByCompany.getOrDefault(companyId, 0L),
          companyInfos.size(),
          pending,
          inProgress,
          completed,
          classCount,
          activeClassCount,
          studentCount,
          examCount,
          issuedCertCount,
          subsidyCount,
          trainings
      ));
    }

    result.sort(Comparator
        .comparingInt((TrainDashboardVO.CompanyOverview c) -> companyRank(c))
        .thenComparing(Comparator.comparingLong(TrainDashboardVO.CompanyOverview::inProgressCount).reversed())
        .thenComparing(Comparator.comparingLong(TrainDashboardVO.CompanyOverview::studentCount).reversed())
        .thenComparing(Comparator.comparingLong(TrainDashboardVO.CompanyOverview::trainingCount).reversed())
        .thenComparing(TrainDashboardVO.CompanyOverview::companyName, Comparator.nullsLast(String::compareTo)));
    return result;
  }

  private static Long normalizeCompanyId(Long companyId) {
    return companyId != null && companyId > 0 ? companyId : null;
  }

  /** 0=正在培训，1=有历史开班，2=未开班 */
  private static int companyRank(TrainDashboardVO.CompanyOverview company) {
    if (company.inProgressCount() > 0 || company.activeClassCount() > 0) {
      return 0;
    }
    if (company.trainingCount() > 0 || company.studentCount() > 0 || company.classCount() > 0) {
      return 1;
    }
    return 2;
  }

  @Transactional(readOnly = true)
  public List<TrainDashboardVO.MilestoneItem> getMilestones() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
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
      String stage = resolveStageLabel(info);
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

  private static String resolveStageLabel(TrainInfoListEntity info) {
    if (info == null || info.getStatus() == null) {
      return "待开班";
    }
    return switch (info.getStatus()) {
      case 0 -> "待开班";
      case 1 -> "培训中";
      case 2 -> "已结业";
      default -> "待开班";
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
    List<TrainInfoListEntity> infos = infoListMapper.selectList(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0));
    Map<String, Long> distribution = new LinkedHashMap<>();
    distribution.put("待开班", 0L);
    distribution.put("培训中", 0L);
    distribution.put("已结业", 0L);
    for (TrainInfoListEntity info : infos) {
      distribution.merge(resolveStageLabel(info), 1L, Long::sum);
    }
    return distribution;
  }
}
